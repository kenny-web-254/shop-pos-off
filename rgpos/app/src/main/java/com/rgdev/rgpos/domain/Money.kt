package com.rgdev.rgpos.domain

/**
 * All money in RG POS is a Long number of CENTS (1 KSh = 100). No Float/Double anywhere.
 * Unit costs use "micro-KSh" (1 cent = 10,000 micro) so KSh 720 / 72 pieces stays exact.
 */
object Money {
    const val MICRO_PER_CENT = 10_000L

    fun format(cents: Long): String {
        val neg = cents < 0
        val abs = Math.abs(cents)
        val whole = (abs / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        val frac = (abs % 100).toString().padStart(2, '0')
        return (if (neg) "-" else "") + "KSh $whole.$frac"
    }

    /** Half-up integer division. numerator >= 0, denominator > 0. */
    fun divRound(numerator: Long, denominator: Long): Long {
        require(numerator >= 0 && denominator > 0) { "divRound needs numerator>=0 and denominator>0" }
        val q = numerator / denominator
        val r = numerator % denominator
        return if (r * 2 >= denominator) q + 1 else q
    }
}
