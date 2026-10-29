package com.example.expense_tracker.domain.model

data class Product(
    val id: String,
    val name: String,
    val nameKey: String,
    val userId: String,
    val categoryId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)