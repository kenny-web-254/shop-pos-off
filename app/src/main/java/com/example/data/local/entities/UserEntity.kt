package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val fullName: String,
    val role: String, // "Owner", "Manager", "Cashier", "Storekeeper"
    val pin: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
