package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val creditLimit: Double = 0.0,
    val outstandingCredit: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerId: Long,
    val customerName: String,
    val type: String, // "SaleCredit" or "Repayment"
    val amount: Double, // positive for credit addition, negative or positive repayment
    val paymentMethod: String = "", // "Cash", "M-Pesa" for repayment
    val reference: String = "", // Receipt # or M-Pesa code
    val balanceAfter: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
