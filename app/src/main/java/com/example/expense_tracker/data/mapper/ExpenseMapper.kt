package com.example.expense_tracker.data.mapper

import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.domain.model.Expense
import com.example.expense_tracker.data.local.dao.RecentExpenseRow
import com.example.expense_tracker.ui.main.IconType
import com.example.expense_tracker.ui.main.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

fun RecentExpenseRow.toTransaction(): Transaction {
    val dateLabel = formatRelativeDate(date)
    return Transaction(
        id = id,
        title = productName,
        category = categoryName,
        date = dateLabel,
        amount = amount / 100.0,
        iconType = guessIconType(categoryName),
        isExpense = true
    )
}

private fun formatRelativeDate(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestampMs }

    val sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> "Dzisiaj"
        isYesterday -> "Wczoraj"
        else -> SimpleDateFormat("dd MMM", Locale("pl", "PL"))
            .format(target.time)
            .uppercase(Locale("pl", "PL"))
    }
}

private fun guessIconType(categoryName: String): IconType {
    val key = categoryName.lowercase(Locale("pl", "PL"))
    return when {
        key.contains("jedzen") || key.contains("restaur") -> IconType.FOOD
        key.contains("transport") || key.contains("paliw") -> IconType.TRANSPORT
        key.contains("zakup") -> IconType.SHOPPING
        key.contains("dom") || key.contains("czynsz") -> IconType.HOME
        key.contains("subskryp") || key.contains("abonament") -> IconType.SUBSCRIPTION
        key.contains("telefon") || key.contains("komórk") -> IconType.PHONE
        else -> IconType.SHOPPING
    }
}