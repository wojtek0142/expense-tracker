package com.example.expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String, // UUID kategorii

    val name: String,

    val is_builtin: Boolean = false,

    // null dla kategorii wbudowanych
    val user_id: String? = null,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val serverVersion: Int = 0,
    val isSynced: Boolean = false
)