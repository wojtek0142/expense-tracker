package com.example.expense_tracker.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Odczyt userId (Flow)
    val userIdFlow: Flow<String?> = dataStore.data.map { it[USER_ID_KEY] }

    // Synchroniczne pobranie userId (dla ViewModel)
    suspend fun getUserId(): String? = userIdFlow.first()

    // Zapisz przy logowaniu
    suspend fun saveUserId(userId: String) {
        dataStore.edit { it[USER_ID_KEY] = userId }
    }

    // Wyczyść przy wylogowaniu
    suspend fun clear() {
        dataStore.edit { it.remove(USER_ID_KEY) }
    }

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }
}