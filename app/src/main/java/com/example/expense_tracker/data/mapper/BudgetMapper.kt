package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.BudgetEntity
import com.example.expense_tracker.domain.model.Budget

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        userId = user_id,
        month = month,
        amount = amount,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        user_id = userId,
        month = month,
        amount = amount,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}