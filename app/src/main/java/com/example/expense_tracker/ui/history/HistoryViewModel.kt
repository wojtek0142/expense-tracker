package com.example.expense_tracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.ExpenseRepository
import com.example.expense_tracker.ui.main.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val allTransactions: List<Transaction> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    private val locale = Locale("pl", "PL")

    val filtered: List<Transaction>
        get() {
            val q = query.trim().lowercase(locale)
            if (q.isEmpty()) return allTransactions
            return allTransactions.filter {
                it.title.lowercase(locale).contains(q) || it.category.lowercase(locale).contains(q)
            }
        }

    val filteredTotal: Double get() = filtered.sumOf { it.amount }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Brak zalogowanego użytkownika.") }
                return@launch
            }
            // LIMIT -1 w SQLite = bez limitu → wszystkie wydatki, od najnowszych
            expenseRepository.observeRecentTransactions(userId, limit = -1)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Błąd ładowania wydatków: ${e.message}")
                    }
                }
                .collect { list ->
                    _uiState.update { it.copy(allTransactions = list, isLoading = false) }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }
}