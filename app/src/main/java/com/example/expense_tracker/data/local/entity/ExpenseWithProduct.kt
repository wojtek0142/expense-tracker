package com.example.expense_tracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithProduct(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: ProductEntity
)