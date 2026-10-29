package com.example.expense_tracker.domain.model

data class Category(
    val id: String,
    val name: String,
    val isBuiltin: Boolean = false,
    val userId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)