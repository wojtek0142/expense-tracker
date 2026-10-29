package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Expense
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}