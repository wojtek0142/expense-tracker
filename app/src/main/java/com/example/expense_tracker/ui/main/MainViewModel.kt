package com.example.expense_tracker.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.repository.ReceiptRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class IconType { FOOD, TRANSPORT, SHOPPING, HOME, SUBSCRIPTION, PHONE }
data class BudgetState(
    val remaining: Double = 1819.80,
    val totalBudget: Double = 4200.00,
    val spent: Double = 2380.20,
    val daysLeft: Int = 11,
    val dailyAverage: Double = 85.00,
    val canSpendDaily: Double = 145.00,
    val percentageUsed: Int = 87
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
    val userName: String = "Aniu",
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
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState(isLoading = true))
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, errorMessage = null)}
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

            _uiState.update { currentState ->
                currentState.copy(
                    recentTransactions = transactions,
                    upcomingPayments = payments
                )
            }
        }
    }
}