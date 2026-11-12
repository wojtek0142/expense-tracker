package com.example.expense_tracker.data.repository

import android.util.Log
import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.dao.CategoryTotal
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Expense
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import com.example.expense_tracker.data.mapper.toTransaction
import com.example.expense_tracker.ui.main.Transaction

class ExpenseRepository @Inject constructor(
    private val appDao: AppDao
) {
    fun getExpensesForUser(userId: String): Flow<List<Expense>> =
        appDao.getExpensesForUser(userId).map { expenses ->
            expenses.map { it.toDomain() }
        }

    fun getExpensesForProduct(
        userId: String,
        productId: String
    ): Flow<List<Expense>> =
        appDao.getExpensesByProduct(userId, productId).map { expenses ->
            expenses.map { it.toDomain() }
        }

    suspend fun addExpense(expense: Expense) {
        appDao.insertExpense(expense.toEntity())
    }

    suspend fun addExpenses(expenses: List<Expense>) {
        appDao.insertExpenses(expenses.map { it.toEntity() })
    }

    suspend fun updateExpense(expense: Expense) {
        appDao.updateExpense(expense.toEntity())
    }

    suspend fun deleteExpense(
        expenseId: String,
        userId: String
    ) {
        val now = System.currentTimeMillis()

        appDao.markExpenseDeleted(
            expenseId = expenseId,
            userId = userId,
            deletedAt = now,
            updatedAt = now
        )
    }

    suspend fun getTotalAmountForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Long =
        appDao.getTotalAmountForPeriod(userId, startDate, endDate)

    suspend fun getExpensesPerCategoryForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ) = appDao.getExpensesPerCategoryForPeriod(
        userId = userId,
        startDate = startDate,
        endDate = endDate
    )

    suspend fun getUnsyncedExpenses(userId: String) =
        appDao.getUnsyncedExpenses(userId)

    suspend fun markExpenseSynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    ) {
        appDao.markExpenseSynced(
            id = id,
            userId = userId,
            serverVersion = serverVersion,
            expectedUpdatedAt = expectedUpdatedAt
        )
    }
    suspend fun getExpensesForMonth(
        userId: String,
        year: Int,
        month: Int
    ): List<Expense> {
        val (startDate, endDate) = monthRange(year, month)
        return appDao.getExpensesForMonth(userId, startDate, endDate)
            .map { it.toDomain() }
    }

    fun observeExpenseAmountFromMonthStart(userId: String): Flow<Long> {
        val (start, end) = currentMonthRange()
        return appDao.observeExpenseAmountForRange(userId, start, end)
    }

    fun observeExpenseCountFromMonthStart(userId: String): Flow<Int> {
        val (start, end) = currentMonthRange()
        return appDao.observeExpenseCountForRange(userId, start, end)
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance().apply {
            clear()
            set(
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                1, 0, 0, 0
            )
        }
        val start = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    fun observeExpenseAmountFromPreviousMonthSameDay(userId: String): Flow<Long> {
        val (start, end) = previousMonthSameDayRange()
        return appDao.observeExpenseAmountForRange(userId, start, end)
    }

    fun observeTopCategoryFromMonthStart(userId: String): Flow<CategoryTotal?> {
        val (start, end) = currentMonthRange()
        return appDao.observeTopCategoryForPeriod(userId, start, end)
    }

    fun observeRecentTransactions(
        userId: String,
        limit: Int = 3
    ): Flow<List<Transaction>> =
        appDao.observeRecentExpensesWithProductAndCategory(userId, limit)
            .map { rows -> rows.map { it.toTransaction() } }

}

private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
    val calendar = java.util.Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1, 0, 0, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(java.util.Calendar.MONTH, 1)
    return start to calendar.timeInMillis
}

private fun previousMonthSameDayRange(): Pair<Long, Long> {
    val now = java.util.Calendar.getInstance()

    // Ile dni ma poprzedni miesiąc
    val prev = (now.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.MONTH, -1)
    }
    val daysInPrevMonth = prev.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val todayDay = now.get(java.util.Calendar.DAY_OF_MONTH)
    val targetDay = todayDay.coerceAtMost(daysInPrevMonth) // clamp dla 29-31

    // Start: 1. dzień poprzedniego miesiąca, 00:00
    val startCal = (now.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.MONTH, -1)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val start = startCal.timeInMillis

    // End (exclusive): dzień targetDay+1 w poprzednim miesiącu, 00:00
    val endCal = (startCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.DAY_OF_MONTH, targetDay)
        add(java.util.Calendar.DAY_OF_MONTH, 1) // exclusive
    }
    val end = endCal.timeInMillis

    return start to end
}