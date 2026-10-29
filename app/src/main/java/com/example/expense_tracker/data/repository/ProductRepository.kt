package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.Product
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository @Inject constructor(
    private val appDao: AppDao
) {
    fun getProductsForUser(userId: String): Flow<List<Product>> =
        appDao.getProductsForUser(userId).map { products ->
            products.map { it.toDomain() }
        }

    fun getProductsByCategory(
        userId: String,
        categoryId: String
    ): Flow<List<Product>> =
        appDao.getProductsByCategory(userId, categoryId).map { products ->
            products.map { it.toDomain() }
        }

    suspend fun getProductById(
        id: String,
        userId: String
    ): Product? =
        appDao.getProductById(id, userId)?.toDomain()

    suspend fun searchProducts(
        userId: String,
        query: String
    ): List<Product> =
        appDao.searchProducts(userId, query).map { it.toDomain() }

    suspend fun addProduct(product: Product) {
        appDao.insertProduct(product.toEntity())
    }

    suspend fun updateProduct(product: Product) {
        appDao.updateProduct(product.toEntity())
    }

    suspend fun deleteProduct(
        productId: String,
        userId: String
    ) {
        val now = System.currentTimeMillis()

        appDao.markProductDeleted(
            productId = productId,
            userId = userId,
            deletedAt = now,
            updatedAt = now
        )
    }

    suspend fun getUnsyncedProducts(userId: String) =
        appDao.getUnsyncedProducts(userId)

    suspend fun markProductSynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    ) {
        appDao.markProductSynced(
            id = id,
            userId = userId,
            serverVersion = serverVersion,
            expectedUpdatedAt = expectedUpdatedAt
        )
    }
}