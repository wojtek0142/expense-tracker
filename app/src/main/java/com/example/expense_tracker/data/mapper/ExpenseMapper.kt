package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.domain.model.Expense

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        productId = product_id,
        userId = user_id,
        quantity = quantity.toDouble(),
        amount = amount,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        product_id = productId,
        user_id = userId,
        quantity = quantity.toFloat(),
        amount = amount,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        serverVersion = 0,
        isSynced = false
    )
}