package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.local.dao.AppDao
import com.example.expense_tracker.data.local.entity.UserEntity
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val appDao: AppDao
) {
    suspend fun getUserById(id: String): UserEntity? = appDao.getUserById(id)
    suspend fun insertUser(user: UserEntity) = appDao.insertUser(user)
}