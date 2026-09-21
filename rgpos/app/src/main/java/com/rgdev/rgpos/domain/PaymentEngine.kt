package com.rgdev.rgpos.domain

/**
 * cashTenderedCents = notes/coins handed over (may exceed what is due -> change).
 * mpesaCents / creditCents are exact amounts. Cash applied is whatever is left of the total.
 */
data class PaymentInput(
    val cashTenderedCents: Long,
    val mpesaCents: Long = 0,
    val mpesaRef: String? = null,
    val creditCents: Long = 0
)

data class PaymentResult(
    val cashAppliedCents: Long,
    val changeCents: Long,
    val mpesaCents: Long,
    val mpesaRef: String?,
    val creditCents: Long
)

object PaymentEngine {
    /** Safaricom codes are 10 letters/digits. Change here if you need to accept other formats. */
    private val MPESA_REF = Regex("^[A-Z0-9]{10}$")

    fun normalizeRef(raw: String?): String? = raw?.trim()?.uppercase()?.ifEmpty { null }

    /** Cash still due once M-Pesa and credit are known - use to prefill the cash field. */
    fun cashDue(totalCents: Long, mpesaCents: Long, creditCents: Long): Long =
        maxOf(totalCents - mpesaCents - creditCents, 0L)

    /**
     * Enforces: cash + M-Pesa + credit == total. Throws with a plain-language message otherwise.
     * creditLimitCents: null = no limit, 0 = no credit allowed.
     */
    fun settle(
        totalCents: Long,
        input: PaymentInput,
        customerId: Long?,
        creditLimitCents: Long?,
        customerBalanceCents: Long
    ): PaymentResult {
        if (totalCents <= 0) throw PaymentException("The sale total must be more than zero.")
        if (input.cashTenderedCents < 0 || input.mpesaCents < 0 || input.creditCents < 0)
            throw PaymentException("Payment amounts can't be negative.")

        if (input.mpesaCents + input.creditCents > totalCents)
            throw PaymentException("M-Pesa and credit are more than the sale total (${Money.format(totalCents)}).")

        val cashApplied = totalCents - input.mpesaCents - input.creditCents

        val ref = normalizeRef(input.mpesaRef)
        if (input.mpesaCents > 0 && (ref == null || !MPESA_REF.matches(ref)))
            throw PaymentException("Enter the 10-character M-Pesa transaction code.")

        if (input.creditCents > 0) {
            if (customerId == null) throw PaymentException("Choose a customer to sell on credit.")
            val newBalance = customerBalanceCents + input.creditCents
            if (creditLimitCents != null && newBalance > creditLimitCents)
                throw CreditLimitException(
                    "Credit limit exceeded. Limit ${Money.format(creditLimitCents)}, owes ${Money.format(customerBalanceCents)}, " +
                        "this sale would make it ${Money.format(newBalance)}."
                )
        }

        if (input.cashTenderedCents < cashApplied)
            throw PaymentException("Cash received is less than cash due (${Money.format(cashApplied)}).")

        return PaymentResult(
            cashAppliedCents = cashApplied,
            changeCents = input.cashTenderedCents - cashApplied,
            mpesaCents = input.mpesaCents,
            mpesaRef = if (input.mpesaCents > 0) ref else null,
            creditCents = input.creditCents
        )
    }
}
