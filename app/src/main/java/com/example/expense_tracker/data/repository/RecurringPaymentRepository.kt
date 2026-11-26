package com.example.expense_tracker.data.repository

import androidx.room.withTransaction
import com.example.expense_tracker.data.local.AppDatabase
import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.dao.RecurringPaymentDao
import com.example.expense_tracker.data.local.entity.CategoryEntity
import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.data.local.entity.ProductEntity
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.RecurringFrequency
import com.example.expense_tracker.domain.model.RecurringPayment
import com.example.expense_tracker.domain.model.nextDueDateAfter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class RecurringPaymentRepository @Inject constructor(
    private val database: AppDatabase,
    private val recurringDao: RecurringPaymentDao,
    private val appDao: AppDao
) {

    fun observeRecurringPayments(userId: String): Flow<List<RecurringPayment>> =
        recurringDao.observeRecurringPayments(userId)
            .map { rows -> rows.map { it.toDomain() } }

    fun observeUpcomingRecurringPayments(
        userId: String,
        limit: Int = 5
    ): Flow<List<RecurringPayment>> =
        recurringDao.observeUpcomingRecurringPayments(userId, limit)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun getRecurringPaymentById(id: String, userId: String): RecurringPayment? =
        recurringDao.getRecurringPaymentById(id, userId)?.toDomain()

    suspend fun addRecurringPayment(payment: RecurringPayment) {
        recurringDao.insertRecurringPayment(payment.toEntity())
    }

    suspend fun updateRecurringPayment(payment: RecurringPayment) {
        recurringDao.updateRecurringPayment(
            payment.copy(updatedAt = System.currentTimeMillis()).toEntity()
        )
    }

    suspend fun deleteRecurringPayment(id: String, userId: String) {
        val now = System.currentTimeMillis()
        recurringDao.markRecurringPaymentDeleted(
            id = id,
            userId = userId,
            deletedAt = now,
            updatedAt = now
        )
    }

    /**
     * Dodaje opłatę cykliczną do listy wydatków (z dzisiejszą datą)
     * i przesuwa termin kolejnej płatności o jeden okres.
     *
     * @return nazwa opłaty lub null, gdy opłata nie istnieje.
     */
    suspend fun registerPayment(paymentId: String, userId: String): String? =
        database.withTransaction {
            val entity = recurringDao.getRecurringPaymentById(paymentId, userId)
                ?: return@withTransaction null

            val now = System.currentTimeMillis()
            val categoryId = resolveCategoryId(entity.category_id, userId, now)
            val productId = resolveProductId(userId, entity.name, categoryId, now)

            appDao.insertExpense(
                ExpenseEntity(
                    id = UUID.randomUUID().toString(),
                    product_id = productId,
                    user_id = userId,
                    quantity = 1f,
                    amount = entity.amount,
                    date = now,
                    createdAt = now,
                    updatedAt = now
                )
            )

            val nextDue = nextDueDateAfter(
                startDate = entity.start_date,
                currentDueDate = entity.next_due_date,
                frequency = RecurringFrequency.fromKey(entity.frequency)
            )

            recurringDao.updateRecurringPayment(
                entity.copy(
                    category_id = categoryId,
                    next_due_date = nextDue,
                    last_paid_date = now,
                    updatedAt = now,
                    isSynced = false
                )
            )

            entity.name
        }

    // Jeśli kategoria opłaty została usunięta – użyj "Bez kategorii"
    private suspend fun resolveCategoryId(categoryId: String, userId: String, now: Long): String {
        appDao.getCategoryById(categoryId)?.let { return it.id }

        val fallbackName = "Bez kategorii"
        appDao.findBuiltinCategoryByName(fallbackName)?.let { return it.id }
        appDao.findUserCategoryByName(userId, fallbackName)?.let { return it.id }

        val newCategory = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = fallbackName,
            is_builtin = false,
            user_id = userId,
            createdAt = now,
            updatedAt = now
        )
        appDao.insertCategory(newCategory)
        return newCategory.id
    }

    // Ta sama logika co w AddExpenseViewModel: produkt po nazwie, tworzony gdy brak
    private suspend fun resolveProductId(
        userId: String,
        name: String,
        categoryId: String,
        now: Long
    ): String {
        val nameKey = name.lowercase(Locale.getDefault())

        val existing = appDao.findProductByName(userId, nameKey)
        if (existing != null) {
            if (existing.category_id != categoryId) {
                appDao.updateProduct(
                    existing.copy(category_id = categoryId, updatedAt = now, isSynced = false)
                )
            }
            return existing.id
        }

        val product = ProductEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            name_key = nameKey,
            user_id = userId,
            category_id = categoryId,
            createdAt = now,
            updatedAt = now
        )
        appDao.insertProduct(product)
        return product.id
    }
}