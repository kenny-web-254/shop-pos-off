package com.rgdev.rgpos.data.repo

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.rgdev.rgpos.data.*
import com.rgdev.rgpos.domain.*

data class NewProduct(
    val name: String,
    val sku: String? = null, val barcode: String? = null, val productCode: String? = null,
    val categoryId: Long? = null, val brand: String? = null, val supplierId: Long? = null,
    /** Exactly one unit must have factorToBase == 1 (the base unit). */
    val units: List<UnitFactor>,
    /** Retail AND wholesale prices, per unit, with optional quantity tiers. Must include a retail price for the base unit. */
    val prices: List<PriceTier>,
    /** Buying price is entered for whichever unit you buy in (e.g. per bale). */
    val buyingUnitId: Long, val buyingPriceCents: Long,
    val openingQuantity: Long = 0, val openingUnitId: Long,
    val minStockBase: Long = 0
)

enum class AdjustmentKind(val sign: Int, val type: StockMovementType) {
    DAMAGED(-1, StockMovementType.DAMAGE),
    EXPIRED(-1, StockMovementType.EXPIRED),
    STOCKTAKE_LOSS(-1, StockMovementType.STOCKTAKE),
    STOCKTAKE_GAIN(1, StockMovementType.STOCKTAKE),
    MANUAL_ADD(1, StockMovementType.ADJUSTMENT),
    MANUAL_REMOVE(-1, StockMovementType.ADJUSTMENT)
}

class ProductRepository(
    private val db: AppDatabase,
    private val audit: AuditRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun createProduct(userId: Long, p: NewProduct): Long = db.withTransaction {
        val user = db.requireUser(userId, Permission.MANAGE_PRODUCTS, "You are not allowed to add products.")

        val name = p.name.trim()
        if (name.isEmpty()) throw ValidationException("Enter the product name.")
        UnitConversionEngine.validateSet(p.units)
        val baseUnit = p.units.first { it.factorToBase == 1L }
        PricingEngine.validateTiers(p.prices)
        val unitIds = p.units.map { it.unitId }.toSet()
        if (p.prices.any { it.unitId !in unitIds }) throw ValidationException("A price uses a unit this product doesn't have.")
        if (p.prices.none { it.unitId == baseUnit.unitId && it.priceType == PriceType.RETAIL })
            throw ValidationException("Set a retail price for ${baseUnit.name}.")
        val buyUnit = p.units.firstOrNull { it.unitId == p.buyingUnitId }
            ?: throw ValidationException("Choose which unit the buying price is for.")
        if (p.buyingPriceCents < 0) throw ValidationException("Buying price can't be negative.")
        val openUnit = p.units.firstOrNull { it.unitId == p.openingUnitId }
            ?: throw ValidationException("Choose which unit the opening stock is counted in.")
        if (p.openingQuantity < 0 || p.minStockBase < 0) throw ValidationException("Stock quantities can't be negative.")

        val sku = clean(p.sku); val barcode = clean(p.barcode); val code = clean(p.productCode)
        if (sku != null && db.productDao().countSku(sku) > 0) throw DuplicateException("Another product already uses SKU $sku.")
        if (barcode != null && db.productDao().countBarcode(barcode) > 0) throw DuplicateException("Another product already uses barcode $barcode.")
        if (code != null && db.productDao().countCode(code) > 0) throw DuplicateException("Another product already uses product code $code.")

        val now = clock()
        val costMicro = ProfitEngine.costMicroPerBase(p.buyingPriceCents, buyUnit.factorToBase)
        val id = try {
            db.productDao().insert(
                ProductEntity(
                    name = name, sku = sku, barcode = barcode, productCode = code,
                    categoryId = p.categoryId, brand = clean(p.brand), supplierId = p.supplierId,
                    baseUnitId = baseUnit.unitId, stockBase = 0, minStockBase = p.minStockBase,
                    avgCostMicro = costMicro, createdAt = now, updatedAt = now
                )
            )
        } catch (e: SQLiteConstraintException) {
            throw DuplicateException("SKU, barcode or product code is already used by another product.")
        }
        db.productUnitDao().insertAll(p.units.map { ProductUnitEntity(productId = id, unitId = it.unitId, factorToBase = it.factorToBase) })
        db.priceDao().insertAll(p.prices.map {
            ProductPriceEntity(productId = id, unitId = it.unitId, priceType = it.priceType, minQty = it.minQty, priceCents = it.priceCents)
        })
        if (p.openingQuantity > 0) {
            val baseQty = UnitConversionEngine.toBase(p.openingQuantity, openUnit.factorToBase)
            db.productDao().increaseStock(id, baseQty, now)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = id, type = StockMovementType.OPENING_STOCK, quantityBase = baseQty, balanceAfterBase = baseQty,
                    reason = "Opening stock", refType = "product", refId = id, userId = user.id, createdAt = now
                )
            )
        }
        audit.log(user.id, "PRODUCT_CREATED", "product", id, name)
        id
    }

    /** Replace a product's retail/wholesale tiers. Old sales keep the price they were sold at (snapshotted on the sale). */
    suspend fun updatePrices(userId: Long, productId: Long, prices: List<PriceTier>) = db.withTransaction {
        val user = db.requireUser(userId, Permission.MANAGE_PRODUCTS, "You are not allowed to change prices.")
        val product = db.productDao().getById(productId) ?: throw ValidationException("Product not found.")
        PricingEngine.validateTiers(prices)
        val factors = db.productUnitDao().factors(productId)
        if (prices.any { p -> factors.none { it.unitId == p.unitId } }) throw ValidationException("A price uses a unit this product doesn't have.")
        if (prices.none { it.unitId == product.baseUnitId && it.priceType == PriceType.RETAIL })
            throw ValidationException("Keep a retail price for the base unit.")
        db.priceDao().deleteForProduct(productId)
        db.priceDao().insertAll(prices.map {
            ProductPriceEntity(productId = productId, unitId = it.unitId, priceType = it.priceType, minQty = it.minQty, priceCents = it.priceCents)
        })
        audit.log(user.id, "PRICE_CHANGED", "product", productId, "${prices.size} price tiers set")
    }

    /** Sets the cost basis used for FUTURE profit calculations (past sales keep the cost they were sold at). */
    suspend fun setBuyingPrice(userId: Long, productId: Long, unitId: Long, priceCents: Long) = db.withTransaction {
        val user = db.requireUser(userId, Permission.SET_BUYING_PRICE, "You are not allowed to change buying prices.")
        if (priceCents < 0) throw ValidationException("Buying price can't be negative.")
        val product = db.productDao().getById(productId) ?: throw ValidationException("Product not found.")
        val unit = db.productUnitDao().factors(productId).firstOrNull { it.unitId == unitId }
            ?: throw ValidationException("That unit isn't set up for this product.")
        val newMicro = ProfitEngine.costMicroPerBase(priceCents, unit.factorToBase)
        db.productDao().setCost(productId, newMicro, clock())
        audit.log(user.id, "BUYING_PRICE_CHANGED", "product", productId, "old=${product.avgCostMicro} new=$newMicro (micro-KSh per base unit)")
    }

    /** Damaged, expired, stocktake and manual corrections. A reason is mandatory. */
    suspend fun adjustStock(
        userId: Long, productId: Long, unitId: Long, quantity: Long, kind: AdjustmentKind, reason: String
    ) = db.withTransaction {
        val user = db.requireUser(userId, Permission.ADJUST_STOCK, "You are not allowed to adjust stock.")
        if (reason.isBlank()) throw ValidationException("Give a reason for this stock change.")
        val product = db.productDao().getById(productId) ?: throw ValidationException("Product not found.")
        val unit = db.productUnitDao().factors(productId).firstOrNull { it.unitId == unitId }
            ?: throw ValidationException("That unit isn't set up for ${product.name}.")
        val baseQty = UnitConversionEngine.toBase(quantity, unit.factorToBase)
        val now = clock()
        if (kind.sign < 0) {
            if (db.productDao().decrementStock(productId, baseQty, now) == 0)
                throw InsufficientStockException(product.name, db.productDao().getStock(productId) ?: 0, baseQty)
        } else {
            db.productDao().increaseStock(productId, baseQty, now)
        }
        val after = db.productDao().getStock(productId) ?: 0
        db.stockMovementDao().insert(
            StockMovementEntity(
                productId = productId, type = kind.type, quantityBase = kind.sign * baseQty, balanceAfterBase = after,
                reason = reason.trim(), refType = "adjustment", refId = null, userId = user.id, createdAt = now
            )
        )
        audit.log(user.id, "STOCK_ADJUSTED", "product", productId, "${kind.name} ${kind.sign * baseQty} base units: ${reason.trim()}")
    }

    /** Products with history are archived, never deleted. */
    suspend fun archive(userId: Long, productId: Long) = db.withTransaction {
        val user = db.requireUser(userId, Permission.MANAGE_PRODUCTS, "You are not allowed to remove products.")
        db.productDao().setActive(productId, false, clock())
        audit.log(user.id, "PRODUCT_ARCHIVED", "product", productId)
    }

    private fun clean(s: String?): String? = s?.trim()?.ifEmpty { null }
}
