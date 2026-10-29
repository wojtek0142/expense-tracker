package com.example.expense_tracker.domain.model

data class Budget(
    val userId: String,
    val month: String,
    val amount: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)