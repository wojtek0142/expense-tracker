package com.example.expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "budgets",
    primaryKeys = ["user_id", "month"],
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
data class BudgetEntity(
    val user_id: String,
    val month: String, // yyyy-MM
    val amount: Long, // kwota w groszach
    val updatedAt: Long,
    val deletedAt: Long?
)