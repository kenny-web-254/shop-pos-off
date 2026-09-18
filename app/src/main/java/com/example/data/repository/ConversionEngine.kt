package com.example.data.repository

import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.UnitConversionEntity

object ConversionEngine {
    /**
     * Resolves the multiplier to convert a given unitName to base units.
     * If the unitName equals the product base unit, multiplier is 1.0.
     * Otherwise, looks up in conversions list.
     */
    fun getMultiplierToBase(
        unitName: String,
        baseUnit: String,
        conversions: List<UnitConversionEntity>
    ): Double {
        if (unitName.equals(baseUnit, ignoreCase = true)) {
            return 1.0
        }
        val match = conversions.firstOrNull { it.unitName.equals(unitName, ignoreCase = true) }
        return match?.multiplierToBase ?: 1.0
    }

    /**
     * Converts a given quantity in unitName into base units.
     */
    fun convertToBaseUnits(
        quantity: Double,
        unitName: String,
        baseUnit: String,
        conversions: List<UnitConversionEntity>
    ): Double {
        val multiplier = getMultiplierToBase(unitName, baseUnit, conversions)
        return quantity * multiplier
    }

    /**
     * Calculates the unit price for a given unit name based on whether it is retail or wholesale,
     * considering any override on the conversion entity, or applying the base unit price * multiplier.
     */
    fun calculateUnitPrice(
        unitName: String,
        isWholesale: Boolean,
        product: ProductEntity,
        conversions: List<UnitConversionEntity>
    ): Double {
        val multiplier = getMultiplierToBase(unitName, product.baseUnit, conversions)
        val conversion = conversions.firstOrNull { it.unitName.equals(unitName, ignoreCase = true) }

        return if (isWholesale) {
            if (conversion != null && conversion.wholesalePriceOverride > 0) {
                conversion.wholesalePriceOverride
            } else {
                product.wholesalePrice * multiplier
            }
        } else {
            if (conversion != null && conversion.retailPriceOverride > 0) {
                conversion.retailPriceOverride
            } else {
                product.retailPrice * multiplier
            }
        }
    }

    /**
     * Formats stock for display (e.g., "281 pieces" or "23 dozens, 5 pieces")
     */
    fun formatStock(stockInBase: Double, baseUnit: String): String {
        return if (stockInBase % 1.0 == 0.0) {
            "${stockInBase.toInt()} $baseUnit"
        } else {
            "%.1f $baseUnit".format(stockInBase)
        }
    }
}
