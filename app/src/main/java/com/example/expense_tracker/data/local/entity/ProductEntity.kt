package com.example.expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("category_id"),
        Index("user_id")
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: String, // UUID produktu

    val name: String,
    val name_key: String,

    val user_id: String,
    val category_id: String,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val serverVersion: Int = 0,
    val isSynced: Boolean = false
)