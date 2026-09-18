package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CashSessionEntity
import com.example.data.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    // Cash session queries
    @Query("SELECT * FROM cash_sessions WHERE status = 'Open' ORDER BY openedAt DESC LIMIT 1")
    fun getCurrentSession(): Flow<CashSessionEntity?>

    @Query("SELECT * FROM cash_sessions WHERE status = 'Open' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getCurrentSessionSync(): CashSessionEntity?

    @Query("SELECT * FROM cash_sessions ORDER BY openedAt DESC")
    fun getAllSessions(): Flow<List<CashSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashSessionEntity)
}
