package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Category
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

    suspend fun getUnsyncedCategories(userId: String) =
        appDao.getUnsyncedCategories(userId)

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