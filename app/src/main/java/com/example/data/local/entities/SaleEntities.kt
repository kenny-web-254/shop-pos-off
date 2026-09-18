package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["customerId"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val receiptNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String, // "Cash", "M-Pesa", "Credit", "Partial"
    val cashReceived: Double = 0.0,
    val changeGiven: Double = 0.0,
    val mpesaRef: String = "",
    val status: String = "Completed", // "Completed", "Refunded", "Partially Refunded"
    val cashierName: String = "Cashier",
    val isWholesale: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitName: String, // Sold unit (e.g. "Piece", "Carton")
    val quantity: Double, // Sold quantity (e.g. 2.0)
    val multiplierToBase: Double, // (e.g. 72.0)
    val baseQuantity: Double, // (e.g. 144.0)
    val unitPrice: Double, // Price per sold unit
    val buyingCostPerBase: Double, // Base unit cost at time of sale
    val discount: Double = 0.0,
    val subtotal: Double,
    val isWholesale: Boolean = false
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["method"]),
        Index(value = ["timestamp"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val method: String, // "Cash", "M-Pesa", "Credit"
    val amount: Double,
    val reference: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "held_sales",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class HeldSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerName: String = "Walk-in Customer",
    val itemsJson: String,
    val subtotal: Double,
    val discount: Double,
    val totalAmount: Double,
    val isWholesale: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
