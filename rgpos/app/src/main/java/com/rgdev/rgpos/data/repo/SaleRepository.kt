package com.rgdev.rgpos.data.repo

import androidx.room.withTransaction
import com.rgdev.rgpos.data.*
import com.rgdev.rgpos.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** quantityBase comes from UnitConversionEngine (e.g. 1 carton -> 72). unitPriceCents is per ONE selling unit. */
data class CartLine(
    val productId: Long, val unitId: Long, val quantityBase: Long,
    val unitPriceCents: Long, val discountCents: Long = 0, val priceSource: PriceSource
)

data class CheckoutRequest(
    val userId: Long, val lines: List<CartLine>, val customerId: Long?,
    val priceType: PriceType, val payment: PaymentInput,
    val cashSessionId: Long? = null, val note: String? = null
)

data class CheckoutResult(
    val saleId: Long, val receiptNo: String, val totalCents: Long,
    val changeCents: Long, val customerBalanceCents: Long?
)

class SaleRepository(
    private val db: AppDatabase,
    private val audit: AuditRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private class Pending(val item: SaleItemEntity, val stockAfter: Long)

    /**
     * One atomic transaction: sale + items + payments + stock deduction + stock movements +
     * customer credit ledger + audit. ANY failure (including insufficient stock on the 10th line)
     * rolls everything back, so a sale is never half-recorded.
     */
    suspend fun checkout(req: CheckoutRequest): CheckoutResult = db.withTransaction {
        if (req.lines.isEmpty()) throw ValidationException("The cart is empty.")
        val user = db.requireUser(req.userId, Permission.RECORD_SALE, "You are not allowed to make sales.")
        val now = clock()

        val customer: CustomerEntity? = if (req.customerId == null) null else
            db.customerDao().getById(req.customerId)?.takeIf { it.active }
                ?: throw ValidationException("Customer not found or archived.")

        val pending = mutableListOf<Pending>()
        var subtotal = 0L; var discounts = 0L; var cogs = 0L

        for (line in req.lines) {
            if (line.quantityBase <= 0) throw ValidationException("Quantity must be more than zero.")
            val product = db.productDao().getById(line.productId)?.takeIf { it.active }
                ?: throw ValidationException("A product in the cart was not found or is archived.")
            val units = db.productUnitDao().factors(product.id)
            val unit = units.firstOrNull { it.unitId == line.unitId }
                ?: throw ValidationException("${product.name} can't be sold by that unit.")
            val base = units.firstOrNull { it.unitId == product.baseUnitId }
                ?: throw ValidationException("${product.name} has no base unit. Fix it in Inventory.")

            // Never trust the price sent by the UI.
            if (line.priceSource == PriceSource.MANUAL) {
                if (!RolePermissions.can(user.role, Permission.OVERRIDE_PRICE))
                    throw PermissionDeniedException("You are not allowed to change prices. Ask a manager.")
                if (line.unitPriceCents <= 0) throw ValidationException("Price must be more than zero.")
            } else {
                val tiers = db.priceDao().tiers(product.id).map { PriceTier(it.unitId, it.priceType, it.minQty, it.priceCents) }
                val customerPrice = customer?.let { db.priceDao().customerPrice(it.id, product.id, line.unitId) }
                val resolved = PricingEngine.resolve(tiers, unit, base, line.quantityBase, req.priceType, customerPrice)
                if (resolved.unitPriceCents != line.unitPriceCents)
                    throw ValidationException("The price of ${product.name} changed. Please refresh the cart.")
            }

            val gross = PricingEngine.lineTotal(line.unitPriceCents, line.quantityBase, unit.factorToBase)
            if (line.discountCents < 0 || line.discountCents > gross)
                throw ValidationException("Discount on ${product.name} must be between 0 and ${Money.format(gross)}.")
            val net = gross - line.discountCents
            val cost = ProfitEngine.cogsCents(line.quantityBase, product.avgCostMicro)

            // Guarded update: refuses (0 rows) if it would go below zero.
            if (db.productDao().decrementStock(product.id, line.quantityBase, now) == 0)
                throw InsufficientStockException(product.name, db.productDao().getStock(product.id) ?: 0, line.quantityBase)
            val stockAfter = db.productDao().getStock(product.id) ?: 0

            subtotal += gross; discounts += line.discountCents; cogs += cost
            pending += Pending(
                SaleItemEntity(
                    saleId = 0, productId = product.id, productName = product.name,
                    unitId = unit.unitId, unitName = unit.name, unitFactor = unit.factorToBase,
                    quantityBase = line.quantityBase, unitPriceCents = line.unitPriceCents,
                    discountCents = line.discountCents, lineTotalCents = net, costCents = cost, priceSource = line.priceSource
                ),
                stockAfter
            )
        }

        val total = subtotal - discounts
        val pay = PaymentEngine.settle(total, req.payment, customer?.id, customer?.creditLimitCents, customer?.balanceCents ?: 0)
        if (pay.mpesaCents > 0 && db.paymentDao().countByRef(pay.mpesaRef!!) > 0)
            throw PaymentException("M-Pesa code ${pay.mpesaRef} was already used on another payment.")

        val receiptNo = nextReceiptNo(now)
        val saleId = db.saleDao().insert(
            SaleEntity(
                receiptNo = receiptNo, customerId = customer?.id, userId = user.id, cashSessionId = req.cashSessionId,
                priceType = req.priceType, subtotalCents = subtotal, discountCents = discounts, totalCents = total,
                cashAppliedCents = pay.cashAppliedCents, cashTenderedCents = req.payment.cashTenderedCents, changeCents = pay.changeCents,
                mpesaCents = pay.mpesaCents, creditCents = pay.creditCents, costCents = cogs, note = req.note, createdAt = now
            )
        )
        db.saleDao().insertItems(pending.map { it.item.copy(saleId = saleId) })
        db.stockMovementDao().insertAll(pending.map {
            StockMovementEntity(
                productId = it.item.productId, type = StockMovementType.SALE, quantityBase = -it.item.quantityBase,
                balanceAfterBase = it.stockAfter, reason = "Sale $receiptNo", refType = "sale", refId = saleId,
                userId = user.id, createdAt = now
            )
        })
        if (pay.cashAppliedCents > 0) db.paymentDao().insert(
            PaymentEntity(saleId = saleId, customerId = customer?.id, kind = PaymentKind.SALE, method = PaymentMethod.CASH,
                amountCents = pay.cashAppliedCents, cashSessionId = req.cashSessionId, userId = user.id, createdAt = now)
        )
        if (pay.mpesaCents > 0) db.paymentDao().insert(
            PaymentEntity(saleId = saleId, customerId = customer?.id, kind = PaymentKind.SALE, method = PaymentMethod.MPESA,
                amountCents = pay.mpesaCents, mpesaRef = pay.mpesaRef, cashSessionId = req.cashSessionId, userId = user.id, createdAt = now)
        )

        var balanceAfter: Long? = null
        if (pay.creditCents > 0 && customer != null) {
            db.customerDao().applyBalanceDelta(customer.id, pay.creditCents, now)
            balanceAfter = db.customerDao().balance(customer.id) ?: 0
            db.customerTransactionDao().insert(
                CustomerTransactionEntity(
                    customerId = customer.id, type = CustomerTxType.CREDIT_SALE, amountCents = pay.creditCents,
                    balanceAfterCents = balanceAfter, saleId = saleId, note = "Sale $receiptNo", userId = user.id, createdAt = now
                )
            )
        }
        audit.log(user.id, "SALE_CREATED", "sale", saleId, "receipt=$receiptNo total=$total cash=${pay.cashAppliedCents} mpesa=${pay.mpesaCents} credit=${pay.creditCents}")
        CheckoutResult(saleId, receiptNo, total, pay.changeCents, balanceAfter ?: customer?.balanceCents)
    }

    /** RG-YYMMDD-0001, restarting each day (Nairobi time). Runs inside the sale transaction. */
    private suspend fun nextReceiptNo(now: Long): String {
        val day = Instant.ofEpochMilli(now).atZone(NAIROBI).toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
        db.receiptCounterDao().ensure(ReceiptCounterEntity(day, 0))
        db.receiptCounterDao().bump(day)
        val n = db.receiptCounterDao().current(day)
        return "RG-${day.substring(2)}-${n.toString().padStart(4, '0')}"
    }

    private companion object { val NAIROBI: ZoneId = ZoneId.of("Africa/Nairobi") }
}
