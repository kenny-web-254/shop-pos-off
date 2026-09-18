package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["category"]),
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val category: String, // "Rent", "Utilities", "Transport", "Salaries", "Supplies", "Other"
    val description: String,
    val staffMember: String = "Staff",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cash_sessions",
    indices = [
        Index(value = ["openedAt"]),
        Index(value = ["status"])
    ]
)
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val cashierName: String = "Cashier",
    val openingCash: Double,
    val closingCash: Double? = null,
    val expectedCash: Double? = null,
    val difference: Double? = null,
    val status: String = "Open", // "Open", "Closed"
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val notes: String = ""
)
