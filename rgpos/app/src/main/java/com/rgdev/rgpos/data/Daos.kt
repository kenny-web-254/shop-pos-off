package com.rgdev.rgpos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rgdev.rgpos.domain.UnitFactor

@Dao interface UserDao {
    @Insert suspend fun insert(user: UserEntity): Long
    @Query("SELECT * FROM users WHERE id = :id") suspend fun getById(id: Long): UserEntity?
}

@Dao interface UnitDao {
    @Insert suspend fun insert(unit: UnitEntity): Long
    @Query("SELECT * FROM units ORDER BY name") suspend fun all(): List<UnitEntity>
}

@Dao interface ProductDao {
    @Insert suspend fun insert(p: ProductEntity): Long
    @Query("SELECT * FROM products WHERE id = :id") suspend fun getById(id: Long): ProductEntity?
    @Query("SELECT stockBase FROM products WHERE id = :id") suspend fun getStock(id: Long): Long?

    /** Guarded: only succeeds if enough stock. Returns rows changed (0 = refused). */
    @Query("UPDATE products SET stockBase = stockBase - :qty, updatedAt = :now WHERE id = :id AND active = 1 AND stockBase >= :qty")
    suspend fun decrementStock(id: Long, qty: Long, now: Long): Int

    @Query("UPDATE products SET stockBase = stockBase + :qty, updatedAt = :now WHERE id = :id")
    suspend fun increaseStock(id: Long, qty: Long, now: Long): Int

    @Query("UPDATE products SET stockBase = stockBase + :qty, avgCostMicro = :avgCostMicro, updatedAt = :now WHERE id = :id")
    suspend fun applyReceipt(id: Long, qty: Long, avgCostMicro: Long, now: Long): Int

    @Query("UPDATE products SET avgCostMicro = :avgCostMicro, updatedAt = :now WHERE id = :id")
    suspend fun setCost(id: Long, avgCostMicro: Long, now: Long): Int

    @Query("UPDATE products SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long): Int

    @Query("SELECT COUNT(*) FROM products WHERE sku = :v") suspend fun countSku(v: String): Int
    @Query("SELECT COUNT(*) FROM products WHERE barcode = :v") suspend fun countBarcode(v: String): Int
    @Query("SELECT COUNT(*) FROM products WHERE productCode = :v") suspend fun countCode(v: String): Int

    @Query("SELECT * FROM products WHERE active = 1 AND barcode = :code LIMIT 1")
    suspend fun byBarcode(code: String): ProductEntity?

    @Query(
        "SELECT * FROM products WHERE active = 1 AND (name LIKE '%' || :q || '%' OR sku LIKE :q || '%' " +
            "OR barcode = :q OR productCode LIKE :q || '%' OR brand LIKE :q || '%') ORDER BY name LIMIT :limit"
    )
    suspend fun search(q: String, limit: Int = 50): List<ProductEntity>

    @Query("SELECT * FROM products WHERE active = 1 AND stockBase <= minStockBase ORDER BY stockBase")
    suspend fun lowStock(): List<ProductEntity>
}

@Dao interface ProductUnitDao {
    @Insert suspend fun insertAll(units: List<ProductUnitEntity>)

    @Query(
        "SELECT pu.unitId AS unitId, u.name AS name, pu.factorToBase AS factorToBase " +
            "FROM product_units pu JOIN units u ON u.id = pu.unitId WHERE pu.productId = :productId"
    )
    suspend fun factors(productId: Long): List<UnitFactor>
}

@Dao interface PriceDao {
    @Insert suspend fun insertAll(prices: List<ProductPriceEntity>)
    @Query("SELECT * FROM product_prices WHERE productId = :productId") suspend fun tiers(productId: Long): List<ProductPriceEntity>
    @Query("DELETE FROM product_prices WHERE productId = :productId") suspend fun deleteForProduct(productId: Long)
    @Query("SELECT priceCents FROM customer_prices WHERE customerId = :customerId AND productId = :productId AND unitId = :unitId")
    suspend fun customerPrice(customerId: Long, productId: Long, unitId: Long): Long?
}

@Dao interface StockMovementDao {
    @Insert suspend fun insert(m: StockMovementEntity): Long
    @Insert suspend fun insertAll(list: List<StockMovementEntity>)
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun forProduct(productId: Long, limit: Int = 200): List<StockMovementEntity>

    /** Products whose stored stock disagrees with the sum of their movements. Should always be empty. */
    @Query(
        "SELECT p.id FROM products p WHERE p.stockBase != " +
            "COALESCE((SELECT SUM(m.quantityBase) FROM stock_movements m WHERE m.productId = p.id), 0)"
    )
    suspend fun integrityMismatches(): List<Long>
}

@Dao interface CustomerDao {
    @Insert suspend fun insert(c: CustomerEntity): Long
    @Query("SELECT * FROM customers WHERE id = :id") suspend fun getById(id: Long): CustomerEntity?
    @Query("SELECT balanceCents FROM customers WHERE id = :id") suspend fun balance(id: Long): Long?
    @Query("UPDATE customers SET balanceCents = balanceCents + :delta, updatedAt = :now WHERE id = :id")
    suspend fun applyBalanceDelta(id: Long, delta: Long, now: Long): Int
}

@Dao interface CustomerTransactionDao { @Insert suspend fun insert(t: CustomerTransactionEntity): Long }

@Dao interface SupplierDao {
    @Insert suspend fun insert(s: SupplierEntity): Long
    @Query("SELECT * FROM suppliers WHERE id = :id") suspend fun getById(id: Long): SupplierEntity?
    @Query("SELECT balanceCents FROM suppliers WHERE id = :id") suspend fun balance(id: Long): Long?
    @Query("UPDATE suppliers SET balanceCents = balanceCents + :delta, updatedAt = :now WHERE id = :id")
    suspend fun applyBalanceDelta(id: Long, delta: Long, now: Long): Int
}

@Dao interface SupplierTransactionDao { @Insert suspend fun insert(t: SupplierTransactionEntity): Long }

@Dao interface PurchaseDao {
    @Insert suspend fun insert(p: PurchaseEntity): Long
    @Insert suspend fun insertItems(items: List<PurchaseItemEntity>)
}

@Dao interface SaleDao {
    @Insert suspend fun insert(s: SaleEntity): Long
    @Insert suspend fun insertItems(items: List<SaleItemEntity>)
}

@Dao interface PaymentDao {
    @Insert suspend fun insert(p: PaymentEntity): Long
    @Query("SELECT COUNT(*) FROM payments WHERE mpesaRef = :ref") suspend fun countByRef(ref: String): Int
}

@Dao interface ReceiptCounterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun ensure(row: ReceiptCounterEntity)
    @Query("UPDATE receipt_counters SET last = last + 1 WHERE day = :day") suspend fun bump(day: String): Int
    @Query("SELECT last FROM receipt_counters WHERE day = :day") suspend fun current(day: String): Int
}

@Dao interface AuditDao {
    @Insert suspend fun insert(a: AuditLogEntity): Long
    @Query("SELECT hash FROM audit_logs ORDER BY id DESC LIMIT 1") suspend fun lastHash(): String?
    @Query("SELECT * FROM audit_logs ORDER BY id ASC") suspend fun allOrdered(): List<AuditLogEntity>
    // Deliberately NO update/delete methods, and DB triggers block them too.
}
