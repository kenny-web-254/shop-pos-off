package com.rgdev.rgpos.domain

/** Cost is tracked per BASE unit in micro-KSh using weighted-average cost. Profit = revenue - actual cost. */
object ProfitEngine {

    /** Buying price of one [factor]-sized unit (e.g. KSh 720 per carton of 72) -> cost per base unit. */
    fun costMicroPerBase(unitCostCents: Long, factor: Long): Long =
        Money.divRound(Math.multiplyExact(unitCostCents, Money.MICRO_PER_CENT), factor)

    /** Cost of goods sold for a line, in cents. */
    fun cogsCents(baseQty: Long, costMicroPerBase: Long): Long =
        Money.divRound(Math.multiplyExact(baseQty, costMicroPerBase), Money.MICRO_PER_CENT)

    /** Blend the cost of stock on hand with a newly received batch. */
    fun weightedAverage(oldStockBase: Long, oldCostMicro: Long, receivedBase: Long, receivedCostMicro: Long): Long {
        if (oldStockBase <= 0) return receivedCostMicro
        val total = Math.addExact(oldStockBase, receivedBase)
        val value = Math.addExact(
            Math.multiplyExact(oldStockBase, oldCostMicro),
            Math.multiplyExact(receivedBase, receivedCostMicro)
        )
        return Money.divRound(value, total)
    }

    /** For showing "buying price per carton" from the stored base-unit cost. */
    fun buyingPriceCents(costMicroPerBase: Long, factor: Long): Long =
        Money.divRound(Math.multiplyExact(costMicroPerBase, factor), Money.MICRO_PER_CENT)
}
