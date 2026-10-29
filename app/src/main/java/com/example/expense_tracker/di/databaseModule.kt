package com.example.expense_tracker.di

import android.content.Context
import androidx.room.Room
import com.example.expense_tracker.data.local.AppDatabase
import com.example.expense_tracker.data.local.dao.AppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAppDao(
        database: AppDatabase
    ): AppDao {
        return database.appDao()
    }
}