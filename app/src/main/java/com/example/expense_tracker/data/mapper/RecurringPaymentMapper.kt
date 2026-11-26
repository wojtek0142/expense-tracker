package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.RecurringPaymentEntity
import com.example.expense_tracker.data.local.entity.RecurringPaymentWithCategoryName
import com.example.expense_tracker.domain.model.RecurringFrequency
import com.example.expense_tracker.domain.model.RecurringPayment

fun RecurringPaymentEntity.toDomain(categoryName: String? = null): RecurringPayment =
    RecurringPayment(
        id = id,
        userId = user_id,
        categoryId = category_id,
        categoryName = categoryName,
        name = name,
        amount = amount,
        frequency = RecurringFrequency.fromKey(frequency),
        startDate = start_date,
        nextDueDate = next_due_date,
        lastPaidDate = last_paid_date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

fun RecurringPaymentWithCategoryName.toDomain(): RecurringPayment =
    payment.toDomain(categoryName)

fun RecurringPayment.toEntity(): RecurringPaymentEntity =
    RecurringPaymentEntity(
        id = id,
        user_id = userId,
        category_id = categoryId,
        name = name,
        amount = amount,
        frequency = frequency.name,
        start_date = startDate,
        next_due_date = nextDueDate,
        last_paid_date = lastPaidDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        isSynced = false
    )