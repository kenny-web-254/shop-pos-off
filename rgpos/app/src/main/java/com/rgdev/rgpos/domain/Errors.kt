package com.rgdev.rgpos.domain

/** Errors the cashier should see verbatim. Anything else is shown as a generic safe message. */
sealed class RgPosException(message: String) : Exception(message)

class ValidationException(message: String) : RgPosException(message)
class PaymentException(message: String) : RgPosException(message)
class CreditLimitException(message: String) : RgPosException(message)
class PermissionDeniedException(message: String) : RgPosException(message)
class DuplicateException(message: String) : RgPosException(message)
class InsufficientStockException(
    val productName: String,
    val availableBase: Long,
    val requestedBase: Long
) : RgPosException("Not enough stock for $productName (available $availableBase, needed $requestedBase in base units).")

object UserMessage {
    fun of(t: Throwable, action: String = "save"): String = when (t) {
        is RgPosException -> t.message ?: "Something went wrong."
        is ArithmeticException -> "That number is too large."
        else -> "Unable to $action. Nothing was changed."
    }
}
