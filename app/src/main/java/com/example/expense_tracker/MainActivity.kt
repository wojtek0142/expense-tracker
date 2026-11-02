package com.example.expense_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.ui.navigation.AppNavigation
import com.example.expense_tracker.ui.theme.ExpensetrackerTheme
import com.example.expense_tracker.data.repository.UserRepository
import com.example.expense_tracker.data.local.entity.UserEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import com.example.expense_tracker.BuildConfig

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var userRepository: UserRepository   // <-- DODANE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            runBlocking {
                val testUserId = "test-user-123"

                // 1. Zapisz userId w DataStore
                if (sessionManager.getUserId() == null) {
                    sessionManager.saveUserId(testUserId)
                }

                // 2. Upewnij się, że użytkownik istnieje w bazie
                if (userRepository.getUserById(testUserId) == null) {
                    val now = System.currentTimeMillis()
                    userRepository.insertUser(
                        UserEntity(
                            id = testUserId,
                            email = "test@example.com",
                            displayName = "Test User",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }
        }

        setContent {
            ExpensetrackerTheme {
                AppNavigation()
            }
        }
    }
}