package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.HeldSaleEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.SaleEntity
import com.example.data.local.entities.SaleItemEntity
import com.example.data.local.entities.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAllSalesSync(): List<SaleEntity>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSales(limit: Int = 20): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    fun getSaleById(id: Long): Flow<SaleEntity?>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleByIdSync(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getSaleByReceiptNumber(receiptNumber: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSaleSync(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsSync(): List<SaleItemEntity>

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    fun getPaymentsForSale(saleId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    suspend fun getPaymentsForSaleSync(saleId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsSync(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Query("UPDATE sales SET status = :status WHERE id = :saleId")
    suspend fun updateSaleStatus(saleId: Long, status: String)

    // Held sales
    @Query("SELECT * FROM held_sales ORDER BY timestamp DESC")
    fun getHeldSales(): Flow<List<HeldSaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldSale(heldSale: HeldSaleEntity): Long

    @Query("DELETE FROM held_sales WHERE id = :id")
    suspend fun deleteHeldSale(id: Long)
}
