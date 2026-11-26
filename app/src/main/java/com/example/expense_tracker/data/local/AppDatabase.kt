package com.example.expense_tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.dao.RecurringPaymentDao
import com.example.expense_tracker.data.local.entity.BudgetEntity
import com.example.expense_tracker.data.local.entity.CategoryEntity
import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.data.local.entity.ProductEntity
import com.example.expense_tracker.data.local.entity.RecurringPaymentEntity
import com.example.expense_tracker.data.local.entity.UserEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        UserEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        RecurringPaymentEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    abstract fun recurringPaymentDao(): RecurringPaymentDao

    companion object {
        // v1 -> v2: nowa tabela opłat cyklicznych (istniejące dane zostają)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_payments` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `category_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `start_date` INTEGER NOT NULL,
                        `next_due_date` INTEGER NOT NULL,
                        `last_paid_date` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `serverVersion` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_payments_user_id` ON `recurring_payments` (`user_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_payments_category_id` ON `recurring_payments` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_payments_next_due_date` ON `recurring_payments` (`next_due_date`)")
            }
        }
    }
}