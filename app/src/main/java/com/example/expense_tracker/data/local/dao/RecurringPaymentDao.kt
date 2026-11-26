package com.example.expense_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expense_tracker.data.local.entity.RecurringPaymentEntity
import com.example.expense_tracker.data.local.entity.RecurringPaymentWithCategoryName
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayment(payment: RecurringPaymentEntity)

    @Update
    suspend fun updateRecurringPayment(payment: RecurringPaymentEntity)

    @Query("""
        SELECT r.*, c.name AS categoryName
        FROM recurring_payments r
        LEFT JOIN categories c ON c.id = r.category_id AND c.deletedAt IS NULL
        WHERE r.user_id = :userId
          AND r.deletedAt IS NULL
        ORDER BY r.next_due_date ASC, r.name ASC
    """)
    fun observeRecurringPayments(userId: String): Flow<List<RecurringPaymentWithCategoryName>>

    // Najbliższe płatności (zaległe są na początku, bo mają najwcześniejszą datę)
    @Query("""
        SELECT r.*, c.name AS categoryName
        FROM recurring_payments r
        LEFT JOIN categories c ON c.id = r.category_id AND c.deletedAt IS NULL
        WHERE r.user_id = :userId
          AND r.deletedAt IS NULL
        ORDER BY r.next_due_date ASC, r.name ASC
        LIMIT :limit
    """)
    fun observeUpcomingRecurringPayments(
        userId: String,
        limit: Int
    ): Flow<List<RecurringPaymentWithCategoryName>>

    @Query("""
        SELECT * FROM recurring_payments
        WHERE id = :id
          AND user_id = :userId
          AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getRecurringPaymentById(id: String, userId: String): RecurringPaymentEntity?

    @Query("""
        UPDATE recurring_payments
        SET deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :id
          AND user_id = :userId
    """)
    suspend fun markRecurringPaymentDeleted(
        id: String,
        userId: String,
        deletedAt: Long,
        updatedAt: Long
    )

    @Query("""
        SELECT * FROM recurring_payments
        WHERE user_id = :userId
          AND isSynced = 0
        ORDER BY updatedAt ASC
    """)
    suspend fun getUnsyncedRecurringPayments(userId: String): List<RecurringPaymentEntity>
}