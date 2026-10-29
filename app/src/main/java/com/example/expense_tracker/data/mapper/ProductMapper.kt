package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.ProductEntity
import com.example.expense_tracker.domain.model.Product

fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        name = name,
        nameKey = name_key,
        userId = user_id,
        categoryId = category_id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        name_key = nameKey,
        user_id = userId,
        category_id = categoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        serverVersion = 0,
        isSynced = false
    )
}