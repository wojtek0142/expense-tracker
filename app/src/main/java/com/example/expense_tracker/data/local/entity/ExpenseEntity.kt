package com.example.expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
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
        Index("product_id"),
        Index("user_id"),
        Index("date")
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String, // UUID wydatku

    val product_id: String,
    val user_id: String,

    val quantity: Float,

    // Kwota w najmniejszych jednostkach waluty, np. groszach
    val amount: Long,

    // Data jako czas Unix w milisekundach
    val date: Long,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val serverVersion: Int = 0,
    val isSynced: Boolean = false
)