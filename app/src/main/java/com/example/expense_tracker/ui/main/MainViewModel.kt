package com.example.expense_tracker.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ExpenseRow(
    val date: String,
    val productName: String,
    val amount: Double
)

data class MainUiState(
    val budget: String = "2000",
    val spent: Double = 1500.0,
    val recentExpenses: List<ExpenseRow> = emptyList()
)
@HiltViewModel
class MainViewModel @Inject constructor(
    // TODO: wstrzykniecie zaleznosci ReceiptRepository / ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            budget = "2000",
            spent = 1500.0,
            recentExpenses = listOf(
                ExpenseRow("2026-10-15", "Chleb", 4.50),
                ExpenseRow("2026-10-14", "Mleko", 3.20),
                ExpenseRow("2026-10-13", "Masło", 7.99),
                ExpenseRow("2026-10-12", "Ser", 12.40),
                ExpenseRow("2026-10-11", "Szynka", 9.80)
            )
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onBudgetChange(newValue: String) {
        // Pozwalamy tylko na cyfry i kropkę
        if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
            _uiState.update { it.copy(budget = newValue.replace(',', '.')) }
        }
    }

    // TODO: dodanie faktycznych repository
    // fun loadRecentExpenses() { ... }
    // fun loadBudget() { ... }
    // fun loadSpent() { ... }
}