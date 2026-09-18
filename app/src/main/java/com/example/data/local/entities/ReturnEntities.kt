package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "returns",
    indices = [
        Index(value = ["originalReceiptNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val returnNumber: String,
    val originalReceiptNumber: String,
    val saleId: Long,
    val totalRefund: Double,
    val paymentMethod: String = "Cash",
    val authorizedBy: String = "Manager",
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(
            entity = ReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["returnId"]),
        Index(value = ["productId"])
    ]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val returnId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val returnQuantity: Double,
    val multiplierToBase: Double,
    val baseQuantity: Double, // ALWAYS IN BASE UNIT
    val refundAmount: Double
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userName: String,
    val action: String,
    val record: String,
    val timestamp: Long = System.currentTimeMillis()
)
