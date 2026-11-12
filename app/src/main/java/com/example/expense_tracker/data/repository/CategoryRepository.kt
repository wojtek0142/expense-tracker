package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.dao.CategoryTotal
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Category
import com.example.expense_tracker.domain.model.CategoryOption
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository @Inject constructor(
    private val appDao: AppDao
) {
    fun getCategoriesForUser(userId: String): Flow<List<Category>> =
        appDao.getCategoriesForUser(userId).map { categories ->
            categories.map { it.toDomain() }
        }

    suspend fun getCategoryById(id: String): Category? =
        appDao.getCategoryById(id)?.toDomain()

    suspend fun getCategoryCount(userId: String): Int =
        appDao.getCategoryCount(userId)

    suspend fun addCategory(category: Category) {
        appDao.insertCategory(category.toEntity())
    }

    fun observeCategoriesOrderedByUsage(userId: String): Flow<List<CategoryOption>> =
        appDao.observeCategoriesOrderedByUsage(userId).map { rows ->
            rows.map {
                CategoryOption(
                    id = it.id,
                    name = it.name,
                    usageCount = it.usageCount,
                    isBuiltin = it.isBuiltin
                )
            }
        }
    suspend fun updateCategory(category: Category) {
        appDao.updateCategory(category.toEntity())
    }

    suspend fun deleteCategory(
        categoryId: String,
        userId: String
    ) {
        val now = System.currentTimeMillis()

        appDao.markCategoryDeleted(
            categoryId = categoryId,
            userId = userId,
            deletedAt = now,
            updatedAt = now
        )
    }

    suspend fun findBuiltinCategoryByName(name: String): Category? =
        appDao.findBuiltinCategoryByName(name)?.toDomain()

    suspend fun findUserCategoryByName(userId: String, name: String): Category? =
        appDao.findUserCategoryByName(userId, name)?.toDomain()

    suspend fun markCategorySynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    ) {
        appDao.markCategorySynced(
            id = id,
            userId = userId,
            serverVersion = serverVersion,
            expectedUpdatedAt = expectedUpdatedAt
        )
    }
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