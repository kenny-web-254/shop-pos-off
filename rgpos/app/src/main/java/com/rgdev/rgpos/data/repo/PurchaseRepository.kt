package com.rgdev.rgpos.data.repo

import androidx.room.withTransaction
import com.rgdev.rgpos.data.*
import com.rgdev.rgpos.domain.*

data class PurchaseLine(val productId: Long, val unitId: Long, val quantity: Long, val unitCostCents: Long)

data class ReceiveRequest(
    val userId: Long, val supplierId: Long?, val lines: List<PurchaseLine>,
    val paidCents: Long, val invoiceNo: String? = null, val note: String? = null
)

class PurchaseRepository(
    private val db: AppDatabase,
    private val audit: AuditRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /**
     * Receive stock. "5 cartons @ KSh 720" (carton = 72 pieces) adds 360 pieces, sets cost KSh 10/piece
     * (blended with existing stock by weighted average), and writes a stock movement per line.
     * All-or-nothing.
     */
    suspend fun receive(req: ReceiveRequest): Long = db.withTransaction {
        val user = db.requireUser(req.userId, Permission.RECEIVE_STOCK, "You are not allowed to receive stock.")
        if (req.lines.isEmpty()) throw ValidationException("Add at least one item to receive.")
        if (req.paidCents < 0) throw ValidationException("Amount paid can't be negative.")
        val supplier = req.supplierId?.let {
            db.supplierDao().getById(it) ?: throw ValidationException("Supplier not found.")
        }
        val now = clock()

        class Done(val item: PurchaseItemEntity, val stockAfter: Long)
        val done = mutableListOf<Done>()
        var total = 0L

        for (line in req.lines) {
            if (line.unitCostCents < 0) throw ValidationException("Buying price can't be negative.")
            val product = db.productDao().getById(line.productId)?.takeIf { it.active }
                ?: throw ValidationException("A product in this purchase was not found or is archived.")
            val unit = db.productUnitDao().factors(product.id).firstOrNull { it.unitId == line.unitId }
                ?: throw ValidationException("${product.name} can't be bought by that unit.")
            val baseQty = UnitConversionEngine.toBase(line.quantity, unit.factorToBase)
            val costMicro = ProfitEngine.costMicroPerBase(line.unitCostCents, unit.factorToBase)
            val newAvg = ProfitEngine.weightedAverage(product.stockBase, product.avgCostMicro, baseQty, costMicro)
            db.productDao().applyReceipt(product.id, baseQty, newAvg, now)
            val lineTotal = Math.multiplyExact(line.quantity, line.unitCostCents)
            total = Math.addExact(total, lineTotal)
            done += Done(
                PurchaseItemEntity(
                    purchaseId = 0, productId = product.id, unitId = unit.unitId, quantity = line.quantity,
                    unitFactor = unit.factorToBase, quantityBase = baseQty, unitCostCents = line.unitCostCents, lineTotalCents = lineTotal
                ),
                db.productDao().getStock(product.id) ?: 0
            )
        }

        if (req.paidCents > total) throw ValidationException("Amount paid is more than the purchase total (${Money.format(total)}).")
        val owed = total - req.paidCents
        if (supplier == null && owed > 0) throw ValidationException("Choose a supplier to buy on credit.")

        val purchaseId = db.purchaseDao().insert(
            PurchaseEntity(
                supplierId = supplier?.id, invoiceNo = req.invoiceNo?.trim()?.ifEmpty { null },
                status = PurchaseStatus.RECEIVED, totalCents = total, paidCents = req.paidCents,
                note = req.note, userId = user.id, createdAt = now, receivedAt = now
            )
        )
        db.purchaseDao().insertItems(done.map { it.item.copy(purchaseId = purchaseId) })
        db.stockMovementDao().insertAll(done.map {
            StockMovementEntity(
                productId = it.item.productId, type = StockMovementType.PURCHASE, quantityBase = it.item.quantityBase,
                balanceAfterBase = it.stockAfter, reason = "Purchase #$purchaseId", refType = "purchase", refId = purchaseId,
                userId = user.id, createdAt = now
            )
        })
        if (supplier != null && owed > 0) {
            db.supplierDao().applyBalanceDelta(supplier.id, owed, now)
            db.supplierTransactionDao().insert(
                SupplierTransactionEntity(
                    supplierId = supplier.id, amountCents = owed, balanceAfterCents = db.supplierDao().balance(supplier.id) ?: 0,
                    purchaseId = purchaseId, note = "Unpaid part of purchase #$purchaseId", userId = user.id, createdAt = now
                )
            )
        }
        audit.log(user.id, "PURCHASE_RECEIVED", "purchase", purchaseId, "total=$total paid=${req.paidCents}")
        purchaseId
    }
}
