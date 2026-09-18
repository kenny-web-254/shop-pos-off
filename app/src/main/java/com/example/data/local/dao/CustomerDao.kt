package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdSync(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE outstandingCredit > 0 ORDER BY outstandingCredit DESC")
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET outstandingCredit = outstandingCredit + :creditDelta, totalPurchases = totalPurchases + :purchasesDelta WHERE id = :customerId")
    suspend fun addCredit(customerId: Long, creditDelta: Double, purchasesDelta: Double)

    @Query("UPDATE customers SET outstandingCredit = outstandingCredit - :amountPaid, totalPaid = totalPaid + :amountPaid WHERE id = :customerId")
    suspend fun recordRepayment(customerId: Long, amountPaid: Double)

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransactionEntity): Long
}
