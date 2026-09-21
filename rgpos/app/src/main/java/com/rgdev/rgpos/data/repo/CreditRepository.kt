package com.rgdev.rgpos.data.repo

import androidx.room.withTransaction
import com.rgdev.rgpos.data.*
import com.rgdev.rgpos.domain.*

data class RepaymentRequest(
    val userId: Long, val customerId: Long,
    val cashCents: Long, val mpesaCents: Long = 0, val mpesaRef: String? = null,
    val cashSessionId: Long? = null, val note: String? = null
)

class CreditRepository(
    private val db: AppDatabase,
    private val audit: AuditRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /** Customer pays off (part of) what they owe. e.g. owes KSh 10,000, pays 3,000 -> owes 7,000. */
    suspend fun recordRepayment(req: RepaymentRequest): Long = db.withTransaction {
        val user = db.requireUser(req.userId, Permission.RECEIVE_CREDIT_PAYMENT, "You are not allowed to receive payments.")
        if (req.cashCents < 0 || req.mpesaCents < 0) throw PaymentException("Payment amounts can't be negative.")
        val total = Math.addExact(req.cashCents, req.mpesaCents)
        if (total <= 0) throw PaymentException("Enter the amount the customer is paying.")
        val customer = db.customerDao().getById(req.customerId) ?: throw ValidationException("Customer not found.")
        if (total > customer.balanceCents)
            throw PaymentException("That is more than ${customer.name} owes (${Money.format(customer.balanceCents)}).")

        val ref = PaymentEngine.normalizeRef(req.mpesaRef)
        if (req.mpesaCents > 0) {
            if (ref == null || !Regex("^[A-Z0-9]{10}$").matches(ref)) throw PaymentException("Enter the 10-character M-Pesa transaction code.")
            if (db.paymentDao().countByRef(ref) > 0) throw PaymentException("M-Pesa code $ref was already used on another payment.")
        }
        val now = clock()
        if (req.cashCents > 0) db.paymentDao().insert(
            PaymentEntity(customerId = customer.id, kind = PaymentKind.REPAYMENT, method = PaymentMethod.CASH,
                amountCents = req.cashCents, cashSessionId = req.cashSessionId, userId = user.id, createdAt = now)
        )
        if (req.mpesaCents > 0) db.paymentDao().insert(
            PaymentEntity(customerId = customer.id, kind = PaymentKind.REPAYMENT, method = PaymentMethod.MPESA,
                amountCents = req.mpesaCents, mpesaRef = ref, cashSessionId = req.cashSessionId, userId = user.id, createdAt = now)
        )
        db.customerDao().applyBalanceDelta(customer.id, -total, now)
        val txId = db.customerTransactionDao().insert(
            CustomerTransactionEntity(
                customerId = customer.id, type = CustomerTxType.REPAYMENT, amountCents = -total,
                balanceAfterCents = db.customerDao().balance(customer.id) ?: 0, note = req.note ?: "Repayment",
                userId = user.id, createdAt = now
            )
        )
        audit.log(user.id, "CREDIT_PAYMENT_RECORDED", "customer", customer.id, "cash=${req.cashCents} mpesa=${req.mpesaCents}")
        txId
    }
}
