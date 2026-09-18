package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["timestamp"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseNumber: String,
    val supplierId: Long,
    val supplierName: String,
    val totalAmount: Double,
    val paymentStatus: String = "Paid", // "Paid", "Pending"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"])
    ]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val purchaseUnit: String,
    val quantity: Double,
    val multiplierToBase: Double,
    val baseQuantity: Double, // ALWAYS CALCULATED AND STORED
    val costPerUnit: Double,
    val totalCost: Double
)
