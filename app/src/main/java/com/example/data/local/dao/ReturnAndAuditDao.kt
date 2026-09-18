package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.ReturnEntity
import com.example.data.local.entities.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY timestamp DESC")
    fun getAllReturns(): Flow<List<ReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: ReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    fun getItemsForReturn(returnId: Long): Flow<List<ReturnItemEntity>>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    suspend fun getItemsForReturnSync(returnId: Long): List<ReturnItemEntity>
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long
}
