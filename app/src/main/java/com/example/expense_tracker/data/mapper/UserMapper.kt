package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.UserEntity
import com.example.expense_tracker.domain.model.User

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        displayName = displayName,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        serverVersion = 0,
        isSynced = false
    )
}