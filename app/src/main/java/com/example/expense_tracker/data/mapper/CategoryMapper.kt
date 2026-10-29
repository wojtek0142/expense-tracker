package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.CategoryEntity
import com.example.expense_tracker.domain.model.Category

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        isBuiltin = is_builtin,
        userId = user_id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        is_builtin = isBuiltin,
        user_id = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        serverVersion = 0,
        isSynced = false
    )
}