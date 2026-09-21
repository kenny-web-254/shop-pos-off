package com.rgdev.rgpos.domain

enum class PriceType { RETAIL, WHOLESALE }
enum class PriceSource { CUSTOMER_PRICE, TIER, DERIVED_FROM_BASE, MANUAL }

/** Price for ONE [unitId], applying from [minQty] of that unit upward. */
data class PriceTier(val unitId: Long, val priceType: PriceType, val minQty: Long, val priceCents: Long)

data class ResolvedPrice(
    val unitPriceCents: Long,
    val source: PriceSource,
    val usedType: PriceType,
    val fellBackToRetail: Boolean
)

/**
 * Retail and wholesale are just price behaviours on the same product and the same stock.
 *
 * Resolution order for a line:
 *  1. customer-specific price (if any)
 *  2. tier set for the exact unit being sold (highest minQty <= quantity)
 *  3. base-unit tier x unit factor (so a carton with no own price = pieces at the tier for that quantity)
 *  For WHOLESALE, steps 2-3 are tried for wholesale first, then retail (flagged fellBackToRetail).
 */
object PricingEngine {

    fun resolve(
        tiers: List<PriceTier>,
        unit: UnitFactor,
        baseUnit: UnitFactor,
        baseQty: Long,
        requested: PriceType,
        customerPriceCents: Long? = null
    ): ResolvedPrice {
        if (baseQty <= 0) throw ValidationException("Quantity must be more than zero.")
        if (customerPriceCents != null) {
            return ResolvedPrice(customerPriceCents, PriceSource.CUSTOMER_PRICE, requested, false)
        }
        val order = if (requested == PriceType.WHOLESALE) listOf(PriceType.WHOLESALE, PriceType.RETAIL) else listOf(PriceType.RETAIL)
        for (type in order) {
            pick(tiers, unit.unitId, type, baseQty, unit.factorToBase)?.let {
                return ResolvedPrice(it.priceCents, PriceSource.TIER, type, type != requested)
            }
            if (unit.unitId != baseUnit.unitId) {
                pick(tiers, baseUnit.unitId, type, baseQty, 1)?.let {
                    return ResolvedPrice(Math.multiplyExact(it.priceCents, unit.factorToBase), PriceSource.DERIVED_FROM_BASE, type, type != requested)
                }
            }
        }
        throw ValidationException("No ${requested.name.lowercase()} price is set for ${unit.name}. Set one in Inventory.")
    }

    private fun pick(tiers: List<PriceTier>, unitId: Long, type: PriceType, baseQty: Long, factor: Long): PriceTier? {
        val qtyInUnit = maxOf(baseQty / factor, 1L)
        return tiers.filter { it.unitId == unitId && it.priceType == type && it.minQty <= qtyInUnit }.maxByOrNull { it.minQty }
    }

    /** price is per ONE selling unit; quantity is in base units. Exact for whole quantities. */
    fun lineTotal(unitPriceCents: Long, baseQty: Long, factor: Long): Long =
        Money.divRound(Math.multiplyExact(unitPriceCents, baseQty), factor)

    fun validateTiers(tiers: List<PriceTier>) {
        if (tiers.any { it.priceCents <= 0 }) throw ValidationException("Prices must be more than zero.")
        if (tiers.any { it.minQty < 1 }) throw ValidationException("A price tier must start from quantity 1 or more.")
        val groups = tiers.groupBy { it.unitId to it.priceType }
        for ((_, g) in groups) {
            if (g.map { it.minQty }.toSet().size != g.size) throw ValidationException("Two price tiers start at the same quantity.")
            val sorted = g.sortedBy { it.minQty }
            for (i in 1 until sorted.size) {
                if (sorted[i].priceCents > sorted[i - 1].priceCents)
                    throw ValidationException("A bigger quantity can't cost more per unit than a smaller one. Check your price tiers.")
            }
        }
    }
}
