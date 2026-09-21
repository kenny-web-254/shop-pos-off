package com.rgdev.rgpos.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** These encode the worked examples from the RG POS spec. Pure JVM: run with ./gradlew test */
class EngineTests {
    private val piece = UnitFactor(1, "Piece", 1)
    private val dozen = UnitFactor(2, "Dozen", 12)
    private val carton = UnitFactor(3, "Carton", 72)
    private val units = listOf(piece, dozen, carton)

    // ---------- units ----------
    @Test fun purchaseThenSellsMatchesSpec() {
        var stock = UnitConversionEngine.toBase(5, carton.factorToBase)   // buy 5 cartons
        assertEquals(360L, stock)
        stock -= UnitConversionEngine.toBase(1, carton.factorToBase)      // sell 1 carton
        stock -= UnitConversionEngine.toBase(7, piece.factorToBase)       // sell 7 pieces
        assertEquals(281L, stock)
        assertEquals("3 Carton, 5 Dozen, 5 Piece", UnitConversionEngine.describe(281, units))
    }

    @Test fun decimalQuantities() {
        assertEquals(2500L, UnitConversionEngine.toBase(BigDecimal("2.5"), 1000))   // 2.5 kg, base = gram
        assertEquals(36L, UnitConversionEngine.toBase(BigDecimal("0.5"), 72))       // half a carton
        assertThrows(ValidationException::class.java) { UnitConversionEngine.toBase(BigDecimal("0.3"), 72) }
    }

    @Test fun unitSetValidation() {
        UnitConversionEngine.validateSet(units)
        assertThrows(ValidationException::class.java) { UnitConversionEngine.validateSet(listOf(dozen, carton)) }       // no base
        assertThrows(ValidationException::class.java) { UnitConversionEngine.validateSet(listOf(piece, UnitFactor(9, "Each", 1))) } // two bases
        assertThrows(ValidationException::class.java) { UnitConversionEngine.validateSet(listOf(piece, UnitFactor(2, "Bad", 0))) }
    }

    // ---------- pricing ----------
    private val tiers = listOf(
        PriceTier(1, PriceType.RETAIL, 1, 2000), PriceTier(1, PriceType.RETAIL, 12, 1800), PriceTier(1, PriceType.RETAIL, 72, 1600)
    )

    @Test fun quantityTiers() {
        fun p(q: Long) = PricingEngine.resolve(tiers, piece, piece, q, PriceType.RETAIL).unitPriceCents
        assertEquals(2000L, p(1)); assertEquals(2000L, p(11))
        assertEquals(1800L, p(12)); assertEquals(1800L, p(71))
        assertEquals(1600L, p(72)); assertEquals(1600L, p(500))
    }

    @Test fun cartonWithoutOwnPriceDerivesFromBaseTier() {
        val r = PricingEngine.resolve(tiers, carton, piece, 72, PriceType.RETAIL)
        assertEquals(PriceSource.DERIVED_FROM_BASE, r.source)
        assertEquals(115_200L, r.unitPriceCents)   // 72 x KSh 16 = KSh 1,152
    }

    @Test fun wholesaleFallsBackToRetailAndSaysSo() {
        val r = PricingEngine.resolve(tiers, piece, piece, 5, PriceType.WHOLESALE)
        assertTrue(r.fellBackToRetail); assertEquals(2000L, r.unitPriceCents)
        val ws = tiers + PriceTier(1, PriceType.WHOLESALE, 1, 1500)
        val w = PricingEngine.resolve(ws, piece, piece, 5, PriceType.WHOLESALE)
        assertFalse(w.fellBackToRetail); assertEquals(1500L, w.unitPriceCents)
    }

    @Test fun customerPriceWinsAndLineTotalsAreExact() {
        assertEquals(1234L, PricingEngine.resolve(tiers, piece, piece, 5, PriceType.RETAIL, 1234).unitPriceCents)
        assertEquals(30_000L, PricingEngine.lineTotal(1500, 20, 1))          // 20 pieces @ KSh 15
        assertEquals(115_200L, PricingEngine.lineTotal(115_200, 72, 72))     // 1 carton
    }

    @Test fun tierValidation() {
        PricingEngine.validateTiers(tiers)
        val bad = listOf(PriceTier(1, PriceType.RETAIL, 1, 1000), PriceTier(1, PriceType.RETAIL, 12, 2000))
        assertThrows(ValidationException::class.java) { PricingEngine.validateTiers(bad) }
    }

    // ---------- payments ----------
    private val ref = "QGH7X2K9LM"

    @Test fun splitPaymentFromSpec() {
        val r = PaymentEngine.settle(200_000, PaymentInput(50_000, 80_000, ref, 70_000), 7, 500_000, 0)
        assertEquals(50_000L, r.cashAppliedCents); assertEquals(0L, r.changeCents)
        assertEquals(200_000L, r.cashAppliedCents + r.mpesaCents + r.creditCents)
    }

    @Test fun changeIsGiven() {
        val r = PaymentEngine.settle(95_000, PaymentInput(100_000), null, null, 0)
        assertEquals(95_000L, r.cashAppliedCents); assertEquals(5_000L, r.changeCents)
    }

    @Test fun invalidPaymentsAreRejected() {
        val t = 100_000L
        assertThrows(PaymentException::class.java) { PaymentEngine.settle(t, PaymentInput(0, 0, null, 100_000), null, null, 0) }   // credit, no customer
        assertThrows(PaymentException::class.java) { PaymentEngine.settle(t, PaymentInput(0, 100_000, "abc", 0), null, null, 0) } // bad M-Pesa code
        assertThrows(PaymentException::class.java) { PaymentEngine.settle(t, PaymentInput(0, 80_000, ref, 30_000), 1, null, 0) }  // over total
        assertThrows(PaymentException::class.java) { PaymentEngine.settle(t, PaymentInput(50_000), null, null, 0) }               // cash short
        assertThrows(PaymentException::class.java) { PaymentEngine.settle(t, PaymentInput(-1), null, null, 0) }
        assertThrows(CreditLimitException::class.java) { PaymentEngine.settle(t, PaymentInput(0, 0, null, 100_000), 1, 150_000, 100_000) }
        assertThrows(CreditLimitException::class.java) { PaymentEngine.settle(t, PaymentInput(0, 0, null, 100_000), 1, 0L, 0) }  // limit 0 = no credit
    }

    // ---------- profit ----------
    @Test fun profitFromSpec() {
        val costMicro = ProfitEngine.costMicroPerBase(72_000, 72)            // KSh 720 per carton of 72
        assertEquals(10_000_000L, costMicro)                                 // KSh 10 per piece
        val cogs = ProfitEngine.cogsCents(20, costMicro)
        assertEquals(20_000L, cogs)                                          // KSh 200
        val revenue = PricingEngine.lineTotal(1500, 20, 1)                   // 20 pieces @ KSh 15
        assertEquals(10_000L, revenue - cogs)                                // KSh 100 gross profit
        assertEquals(72_000L, ProfitEngine.buyingPriceCents(costMicro, 72))
    }

    @Test fun weightedAverageCost() {
        val a = ProfitEngine.weightedAverage(100, 10_000_000, 100, 20_000_000)
        assertEquals(15_000_000L, a)
        assertEquals(20_000_000L, ProfitEngine.weightedAverage(0, 10_000_000, 50, 20_000_000))
    }

    @Test fun moneyFormatting() {
        assertEquals("KSh 1,250.00", Money.format(125_000))
        assertEquals("KSh 0.05", Money.format(5))
        assertEquals("-KSh 500.00", Money.format(-50_000))
        assertEquals("KSh 1,234,567.89", Money.format(123_456_789))
    }
}
