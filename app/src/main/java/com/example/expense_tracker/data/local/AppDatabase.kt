package com.example.expense_tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.entity.BudgetEntity
import com.example.expense_tracker.data.local.entity.CategoryEntity
import com.example.expense_tracker.data.local.entity.ExpenseEntity
import com.example.expense_tracker.data.local.entity.ProductEntity
import com.example.expense_tracker.data.local.entity.UserEntity

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        UserEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
}