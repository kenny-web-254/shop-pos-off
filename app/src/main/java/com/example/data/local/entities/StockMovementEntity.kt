package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val productName: String,
    val quantityDelta: Double, // +360.0, -72.0, etc. (ALWAYS IN BASE UNIT)
    val unitName: String, // Base unit name
    val resultingStock: Double, // Resulting stock in base unit
    val reason: String, // "Sale", "Purchase", "Damage", "Return", "Stock Take", "Adjustment"
    val reference: String = "", // Receipt #, Purchase #, etc.
    val userName: String = "Staff",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
