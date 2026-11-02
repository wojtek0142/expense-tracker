package com.example.expense_tracker.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.repository.BudgetRepository
import com.example.expense_tracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import com.example.expense_tracker.data.local.datastore.SessionManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

enum class IconType { FOOD, TRANSPORT, SHOPPING, HOME, SUBSCRIPTION, PHONE }
data class BudgetState(
    val remaining: Double = 0.0,
    val totalBudget: Double = 4200.00,
    val spent: Double = 0.0,
    val daysLeft: Int = 0,
    val dailyAverage: Double = 0.0,
    val canSpendDaily: Double = 0.0,
    val percentageUsed: Int = 0,
)

data class UpcomingPayment(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String, // np. "25 PAŹ"
    val subtitle: String, // np. "Co miesiąc"
    val daysLeft: String, // np. "Jutro"
    val isUrgent: Boolean = false
)

data class Transaction(
    // do zmiany pod baze
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val amount: Double,
    val iconType: IconType,
    val isExpense: Boolean = true
)

data class MainScreenState(
    val userName: String = "user",
    val budget: BudgetState = BudgetState(),
    val recentTransactions: List<Transaction> = emptyList(),
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    val topCategory: String = "Jedzenie",
    val topCategoryAmount: Double = 450.30,
    val topCategoryPercent: Int = 19,
    val hasOverduePayment: Boolean = true, // Dla czerwonego alertu na dole
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val BudgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(MainScreenState(isLoading = true))
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()
    val monthlyExpenseCount: StateFlow<Int> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            Log.d("CountDebug", "flatMapLatest START dla userId=$userId")
            expenseRepository.observeExpenseCountFromMonthStart(userId)
                .onEach { Log.d("CountDebug", "Emisja z repo: $it") }
                .onCompletion { Log.d("CountDebug", "Flow repo zakończony") }
        }
        .onStart { Log.d("CountDebug", "stateIn upstream START") }
        .onCompletion { Log.d("CountDebug", "stateIn upstream ZAKOŃCZONY") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = 0
        )

    private val monthlyExpenseAmount: StateFlow<Long> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            expenseRepository.observeExpenseAmountFromMonthStart(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = 0L
        )

    private val monthlyExpenseAmountZloty: StateFlow<Double> = monthlyExpenseAmount
        .map { it / 100.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    init {
        loadData()
        observeMonthlyAmount()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = sessionManager.getUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Brak zalogowanego użytkownika."
                        )
                    }
                    return@launch
                }
                _userId.value = userId

                // Przykładowe dane — do podmiany na dane z repo
                val transactions = listOf(
                    Transaction("1", "Bistro Zielnik", "Jedzenie", "Dzisiaj", 64.50, IconType.FOOD),
                    Transaction("2", "Bolt", "Transport", "Dzisiaj", 24.80, IconType.TRANSPORT),
                    Transaction("3", "Allegro", "Zakupy", "Wczoraj", 129.00, IconType.SHOPPING)
                )
                val payments = listOf(
                    UpcomingPayment("1", "Czynsz", 2050.00, "25 PAŹ", "Co miesiąc", "Jutro", true),
                    UpcomingPayment("2", "Netflix", 43.00, "27 PAŹ", "Co miesiąc", "Za 3 dni"),
                    UpcomingPayment("3", "Telefon", 141.99, "29 PAŹ", "Co miesiąc", "Za 5 dni")
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,             // <-- DODANE
                        recentTransactions = transactions,
                        upcomingPayments = payments
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Błąd: ${e.message}")
                }
            }
        }
    }
    private fun observeMonthlyAmount() {
        viewModelScope.launch {
            monthlyExpenseAmount.collect { totalInGrosze ->
                val totalInZloty = totalInGrosze / 100.0

                _uiState.update { currentState ->
                    val budget = currentState.budget
                    val totalBudget = budget.totalBudget

                    val remaining = (totalBudget - totalInZloty).coerceAtLeast(0.0)
                    val percentageUsed = if (totalBudget > 0) {
                        ((totalInZloty / totalBudget) * 100).toInt().coerceIn(0, 100)
                    } else 0

                    currentState.copy(
                        budget = budget.copy(
                            spent = totalInZloty,          // <-- DODANE
                            remaining = remaining,
                            percentageUsed = percentageUsed,
                        )
                    )
                }
            }
        }
    }
}