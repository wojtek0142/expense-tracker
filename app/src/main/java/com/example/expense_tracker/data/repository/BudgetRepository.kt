package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Budget
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository @Inject constructor(
    private val appDao: AppDao
) {
    fun getAllBudgets(userId: String): Flow<List<Budget>> =
        appDao.getAllBudgets(userId).map { budgets ->
            budgets.map { it.toDomain() }
        }

    suspend fun getBudgetForMonth(
        userId: String,
        month: String
    ): Budget? =
        appDao.getBudgetForMonth(userId, month)?.toDomain()

    suspend fun addBudget(budget: Budget) {
        appDao.insertBudget(budget.toEntity())
    }

    suspend fun updateBudget(budget: Budget) {
        appDao.updateBudget(budget.toEntity())
    }

    suspend fun deleteBudget(
        userId: String,
        month: String
    ) {
        appDao.deleteBudget(userId, month)
    }
}