package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.mapper.toDomain
import com.example.expense_tracker.data.mapper.toEntity
import com.example.expense_tracker.domain.model.User
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val appDao: AppDao
) {
    suspend fun getUserById(userId: String): User? =
        appDao.getUserById(userId)?.toDomain()

    suspend fun addUser(user: User) {
        appDao.insertUser(user.toEntity())
    }

    suspend fun updateUser(user: User) {
        appDao.updateUser(user.toEntity())
    }
}