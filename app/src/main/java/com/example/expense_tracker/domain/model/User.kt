package com.example.expense_tracker.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)