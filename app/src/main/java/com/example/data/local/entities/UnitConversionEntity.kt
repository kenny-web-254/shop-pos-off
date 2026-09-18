package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unit_conversions",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["productId", "unitName"], unique = true)
    ]
)
data class UnitConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val unitName: String, // e.g. "Dozen", "Carton"
    val multiplierToBase: Double, // e.g. 12.0, 72.0
    val retailPriceOverride: Double = 0.0, // If 0.0, calculated as multiplier * base retailPrice
    val wholesalePriceOverride: Double = 0.0 // If 0.0, calculated as multiplier * base wholesalePrice
)
