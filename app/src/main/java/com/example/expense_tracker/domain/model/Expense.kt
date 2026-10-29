package com.example.expense_tracker.domain.model

data class Expense(
    val id: String,
    val productId: String,
    val userId: String,
    val quantity: Double,
    val amount: Long,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)