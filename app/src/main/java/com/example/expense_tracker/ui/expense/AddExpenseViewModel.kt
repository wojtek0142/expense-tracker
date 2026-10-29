
package com.example.expense_tracker.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ExpenseType {
    FIXED,
    CASH
}

// Jeden wydatek na liście podglądowej
data class PendingExpense(
    val id: String = UUID.randomUUID().toString(),
    val amount: String,
    val title: String,
    val date: String,
    val category: String,
    val expenseType: ExpenseType,
    val note: String
)

// Stan ekranu
data class AddExpenseUiState(
    val amount: String = "",
    val title: String = "",
    val date: String = "24.10.2024",
    val category: String = "",
    val expenseType: ExpenseType = ExpenseType.FIXED,
    val note: String = "",
    val pendingExpenses: List<PendingExpense> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun onAmountChange(newAmount: String) {
        if (newAmount.matches(Regex("^\\d*[.,]?\\d*$"))) {
            _uiState.update {
                it.copy(amount = newAmount, errorMessage = null)
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update {
            it.copy(title = newTitle, errorMessage = null)
        }
    }

    fun onDateChange(newDate: String) {
        _uiState.update { it.copy(date = newDate) }
    }

    fun onCategoryChange(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun onExpenseTypeChange(type: ExpenseType) {
        _uiState.update { it.copy(expenseType = type) }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    // Dodaje bieżący formularz do listy podglądowej.
    // Nie zapisuje jeszcze danych do bazy.
    fun addExpenseToList() {
        val state = _uiState.value

        val title = state.title.trim()
        val normalizedAmount = state.amount
            .replace(',', '.')
        val amountValue = normalizedAmount.toDoubleOrNull()

        if (title.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Podaj nazwę wydatku.")
            }
            return
        }

        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0.0) {
            _uiState.update {
                it.copy(errorMessage = "Podaj poprawną kwotę większą od zera.")
            }
            return
        }

        val expense = PendingExpense(
            amount = normalizedAmount,
            title = title,
            date = state.date,
            category = state.category.ifBlank { "Bez kategorii" },
            expenseType = state.expenseType,
            note = state.note.trim()
        )

        _uiState.update {
            it.copy(
                pendingExpenses = it.pendingExpenses + expense,
                amount = "",
                title = "",
                note = "",
                errorMessage = null
            )
        }
    }

    // Usuwa element z listy podglądowej.
    fun removeExpenseFromList(id: String) {
        _uiState.update {
            it.copy(
                pendingExpenses = it.pendingExpenses.filterNot { expense ->
                    expense.id == id
                },
                errorMessage = null
            )
        }
    }

    // Na razie symulacja zatwierdzenia listy.
    // Tutaj później należy podłączyć zapis do ExpenseRepository.
    fun saveAllExpenses() {
        val state = _uiState.value

        if (state.pendingExpenses.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Dodaj przynajmniej jeden wydatek.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            // Tymczasowa symulacja. Nie zapisuje do Room.
            delay(500)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSaved = true
                )
            }
        }
    }
}
