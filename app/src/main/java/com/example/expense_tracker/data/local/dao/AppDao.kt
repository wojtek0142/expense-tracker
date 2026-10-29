
package com.example.expense_tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expense_tracker.data.local.entity.BudgetEntity
import com.example.expense_tracker.data.local.entity.CategoryEntity
import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.data.local.entity.ExpenseWithProduct
import com.example.expense_tracker.data.local.entity.ProductEntity
import com.example.expense_tracker.data.local.entity.ProductWithCategory
import com.example.expense_tracker.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ==========================================
    // USERS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    // ==========================================
    // CATEGORIES
    // Kategorie wbudowane: user_id IS NULL
    // Kategorie użytkownika: user_id = jego UUID
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("""
        SELECT * FROM categories
        WHERE id = :id
          AND deletedAt IS NULL
    """)
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("""
        SELECT * FROM categories
        WHERE (user_id IS NULL OR user_id = :userId)
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun getCategoriesForUser(userId: String): Flow<List<CategoryEntity>>

    @Query("""
        SELECT COUNT(*) FROM categories
        WHERE (user_id IS NULL OR user_id = :userId)
          AND deletedAt IS NULL
    """)
    suspend fun getCategoryCount(userId: String): Int

    // Oznaczenie usunięcia do późniejszej synchronizacji.
    // Nie usuwa fizycznie rekordu.
    @Query("""
        UPDATE categories
        SET deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :categoryId
          AND user_id = :userId
    """)
    suspend fun markCategoryDeleted(
        categoryId: String,
        userId: String,
        deletedAt: Long,
        updatedAt: Long
    )

    // ==========================================
    // PRODUCTS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("""
        SELECT * FROM products
        WHERE id = :id
          AND user_id = :userId
          AND deletedAt IS NULL
    """)
    suspend fun getProductById(
        id: String,
        userId: String
    ): ProductEntity?

    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun getProductsForUser(
        userId: String
    ): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND category_id = :categoryId
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun getProductsByCategory(
        userId: String,
        categoryId: String
    ): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND name_key LIKE '%' || :query || '%'
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    suspend fun searchProducts(
        userId: String,
        query: String
    ): List<ProductEntity>

    @Transaction
    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun getProductsWithCategory(
        userId: String
    ): Flow<List<ProductWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND category_id = :categoryId
          AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun getProductsWithCategoryByCategoryId(
        userId: String,
        categoryId: String
    ): Flow<List<ProductWithCategory>>

    @Query("""
        UPDATE products
        SET deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :productId
          AND user_id = :userId
    """)
    suspend fun markProductDeleted(
        productId: String,
        userId: String,
        deletedAt: Long,
        updatedAt: Long
    )

    // ==========================================
    // EXPENSES
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId
          AND deletedAt IS NULL
        ORDER BY date DESC
    """)
    fun getExpensesForUser(
        userId: String
    ): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId
          AND product_id = :productId
          AND deletedAt IS NULL
        ORDER BY date DESC
    """)
    fun getExpensesByProduct(
        userId: String,
        productId: String
    ): Flow<List<ExpenseEntity>>

    @Transaction
    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId
          AND deletedAt IS NULL
        ORDER BY date DESC
    """)
    fun getExpensesWithProduct(
        userId: String
    ): Flow<List<ExpenseWithProduct>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE user_id = :userId
          AND deletedAt IS NULL
          AND date >= :startDate
          AND date < :endDate
    """)
    suspend fun getTotalAmountForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Long

    @Query("""
        SELECT c.name AS categoryName,
               SUM(e.amount) AS totalAmount
        FROM expenses e
        INNER JOIN products p ON e.product_id = p.id
        INNER JOIN categories c ON p.category_id = c.id
        WHERE e.user_id = :userId
          AND p.user_id = :userId
          AND e.deletedAt IS NULL
          AND p.deletedAt IS NULL
          AND c.deletedAt IS NULL
          AND e.date >= :startDate
          AND e.date < :endDate
        GROUP BY c.id, c.name
        ORDER BY totalAmount DESC
    """)
    suspend fun getExpensesPerCategoryForPeriod(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<CategoryTotal>

    @Query("""
        UPDATE expenses
        SET deletedAt = :deletedAt,
            updatedAt = :updatedAt,
            isSynced = 0
        WHERE id = :expenseId
          AND user_id = :userId
    """)
    suspend fun markExpenseDeleted(
        expenseId: String,
        userId: String,
        deletedAt: Long,
        updatedAt: Long
    )

    // ==========================================
    // SYNCHRONIZATION
    // ==========================================

    @Query("""
        SELECT * FROM products
        WHERE user_id = :userId
          AND isSynced = 0
        ORDER BY updatedAt ASC
    """)
    suspend fun getUnsyncedProducts(
        userId: String
    ): List<ProductEntity>

    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId
          AND isSynced = 0
        ORDER BY updatedAt ASC
    """)
    suspend fun getUnsyncedExpenses(
        userId: String
    ): List<ExpenseEntity>

    @Query("""
        SELECT * FROM categories
        WHERE user_id = :userId
          AND isSynced = 0
        ORDER BY updatedAt ASC
    """)
    suspend fun getUnsyncedCategories(
        userId: String
    ): List<CategoryEntity>

    @Query("""
        UPDATE products
        SET isSynced = 1,
            serverVersion = :serverVersion
        WHERE id = :id
          AND user_id = :userId
          AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun markProductSynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    )

    @Query("""
        UPDATE expenses
        SET isSynced = 1,
            serverVersion = :serverVersion
        WHERE id = :id
          AND user_id = :userId
          AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun markExpenseSynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    )

    @Query("""
        UPDATE categories
        SET isSynced = 1,
            serverVersion = :serverVersion
        WHERE id = :id
          AND user_id = :userId
          AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun markCategorySynced(
        id: String,
        userId: String,
        serverVersion: Int,
        expectedUpdatedAt: Long
    )

    // ==========================================
    // BUDGETS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("""
        SELECT * FROM budgets
        WHERE user_id = :userId
          AND month = :month
    """)
    suspend fun getBudgetForMonth(
        userId: String,
        month: String
    ): BudgetEntity?

    @Query("""
        SELECT * FROM budgets
        WHERE user_id = :userId
        ORDER BY month DESC
    """)
    fun getAllBudgets(
        userId: String
    ): Flow<List<BudgetEntity>>

    @Query("""
        DELETE FROM budgets
        WHERE user_id = :userId
          AND month = :month
    """)
    suspend fun deleteBudget(
        userId: String,
        month: String
    )
}

data class CategoryTotal(
    val categoryName: String,
    val totalAmount: Long
)
