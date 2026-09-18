package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String,
    val phone: String,
    val location: String,
    val ownerName: String,
    val currency: String = "KES",
    val allowRetail: Boolean = true,
    val allowWholesale: Boolean = true,
    val allowCredit: Boolean = true,
    val lowStockThreshold: Int = 5,
    val taxRatePercent: Double = 0.0,
    val receiptHeader: String = "RG POS by RGDev",
    val receiptFooter: String = "Thank you for shopping with us.",
    val ownerPin: String,
    val biometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
