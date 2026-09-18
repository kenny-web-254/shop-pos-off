package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["sku"]),
        Index(value = ["categoryId"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "General",
    val brand: String = "",
    val baseUnit: String = "Piece",
    val buyingCost: Double = 0.0, // Buying cost per base unit
    val retailPrice: Double = 0.0, // Retail price per base unit
    val wholesalePrice: Double = 0.0, // Wholesale price per base unit
    val allowWholesale: Boolean = true,
    val minStock: Int = 5,
    val supplierId: Long = 0L,
    val supplierName: String = "",
    val currentStock: Double = 0.0, // CRITICAL: ALWAYS IN BASE UNIT
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
