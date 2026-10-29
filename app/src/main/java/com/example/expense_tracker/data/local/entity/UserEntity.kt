package com.example.expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String, // UUID użytkownika

    val email: String,
    val displayName: String,

    val isActive: Boolean = true,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val serverVersion: Int = 0,
    val isSynced: Boolean = false
)