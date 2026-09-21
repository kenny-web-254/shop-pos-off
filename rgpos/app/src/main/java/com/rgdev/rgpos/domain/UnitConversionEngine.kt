package com.rgdev.rgpos.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** One way a product can be bought/sold. factorToBase: how many BASE units are in one of this unit. */
data class UnitFactor(val unitId: Long, val name: String, val factorToBase: Long)

/**
 * THE only place unit conversion happens. Purchasing, POS, returns, adjustments and reports
 * must all call this. Stock is always stored in the product's base unit (factor 1).
 */
object UnitConversionEngine {

    fun validateSet(units: List<UnitFactor>) {
        if (units.isEmpty()) throw ValidationException("A product needs at least a base unit.")
        if (units.any { it.factorToBase < 1 }) throw ValidationException("Every unit needs a conversion of 1 or more.")
        if (units.map { it.unitId }.toSet().size != units.size) throw ValidationException("The same unit was added twice.")
        if (units.count { it.factorToBase == 1L } != 1)
            throw ValidationException("Exactly one unit must be the base unit (the smallest, worth 1). Every other unit must be a multiple of it.")
    }

    fun toBase(quantity: Long, factor: Long): Long {
        if (quantity <= 0) throw ValidationException("Quantity must be more than zero.")
        return Math.multiplyExact(quantity, factor)
    }

    /** For decimal entry such as 2.5 kg. Must land on a whole number of base units (0.3 carton of 72 does not). */
    fun toBase(quantity: BigDecimal, factor: Long): Long {
        if (quantity.signum() <= 0) throw ValidationException("Quantity must be more than zero.")
        val exact = try {
            quantity.multiply(BigDecimal(factor)).toBigIntegerExact()
        } catch (e: ArithmeticException) {
            throw ValidationException("That quantity doesn't convert to a whole number of base units. Sell it in a smaller unit.")
        }
        if (exact.bitLength() > 62) throw ValidationException("Quantity is too large.")
        return exact.toLong()
    }

    /** Display only (never used for stock maths). */
    fun fromBase(baseQty: Long, factor: Long): BigDecimal =
        BigDecimal(baseQty).divide(BigDecimal(factor), 4, RoundingMode.HALF_UP).stripTrailingZeros()

    /** 281 pieces -> 3 carton, 5 dozen, 5 piece */
    fun breakdown(baseQty: Long, units: List<UnitFactor>): List<Pair<UnitFactor, Long>> {
        require(baseQty >= 0) { "baseQty must not be negative" }
        var remaining = baseQty
        val out = mutableListOf<Pair<UnitFactor, Long>>()
        for (u in units.sortedByDescending { it.factorToBase }) {
            val n = remaining / u.factorToBase
            if (n > 0) { out += u to n; remaining -= n * u.factorToBase }
        }
        return out
    }

    fun describe(baseQty: Long, units: List<UnitFactor>): String {
        val parts = breakdown(baseQty, units)
        return if (parts.isEmpty()) "0" else parts.joinToString(", ") { "${it.second} ${it.first.name}" }
    }
}
