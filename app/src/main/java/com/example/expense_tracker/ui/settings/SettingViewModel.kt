package com.example.expense_tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.BudgetRepository
import com.example.expense_tracker.domain.model.Budget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class BudgetEntry(
    val month: String,          // "yyyy-MM"
    val monthLabel: String,     // "LISTOPAD 2025"
    val amount: Double,         // zł
    val isCurrentMonth: Boolean = false
)

data class SettingsUiState(
    val budgets: List<BudgetEntry> = emptyList(),
    val selectedMonth: String = currentMonthKey(),
    val selectedMonthLabel: String = currentMonthLabel(),
    val amountInput: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun currentMonthKey(): String {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            return String.format(Locale.US, "%04d-%02d", y, m)
        }

        fun currentMonthLabel(): String {
            val cal = Calendar.getInstance()
            val locale = Locale("pl", "PL")
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
                .uppercase(locale)
            return "$monthName ${cal.get(Calendar.YEAR)}"
        }
    }
}

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(errorMessage = "Brak zalogowanego użytkownika.")
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            budgetRepository.getAllBudgets(userId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Błąd ładowania budżetów: ${e.message}"
                        )
                    }
                }
                .collect { budgets ->
                    val nowKey = SettingsUiState.currentMonthKey()
                    val entries = budgets
                        .filter { it.deletedAt == null }
                        .sortedByDescending { it.month }
                        .map { budget ->
                            BudgetEntry(
                                month = budget.month,
                                monthLabel = formatMonthLabel(budget.month),
                                amount = budget.amount / 100.0,
                                isCurrentMonth = budget.month == nowKey
                            )
                        }

                    // Prefill kwoty dla aktualnie wybranego miesiąca
                    val selected = entries.firstOrNull { it.month == _uiState.value.selectedMonth }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budgets = entries,
                            amountInput = selected?.let { e ->
                                String.format(Locale.US, "%.2f", e.amount)
                            } ?: it.amountInput
                        )
                    }
                }
        }
    }

    fun onMonthSelected(monthKey: String) {
        val label = formatMonthLabel(monthKey)
        val existing = _uiState.value.budgets.firstOrNull { it.month == monthKey }
        _uiState.update {
            it.copy(
                selectedMonth = monthKey,
                selectedMonthLabel = label,
                amountInput = existing?.let { e ->
                    String.format(Locale.US, "%.2f", e.amount)
                } ?: "",
                message = null,
                errorMessage = null
            )
        }
    }

    fun onAmountChange(newAmount: String) {
        if (newAmount.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
            _uiState.update { it.copy(amountInput = newAmount, message = null) }
        }
    }

    fun saveBudget() {
        val state = _uiState.value
        val normalized = state.amountInput.replace(',', '.')
        val amount = normalized.toDoubleOrNull()

        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            _uiState.update {
                it.copy(errorMessage = "Podaj poprawną kwotę większą od zera.")
            }
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(errorMessage = "Brak zalogowanego użytkownika.")
                }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }

            try {
                val now = System.currentTimeMillis()
                val amountGrosze = Math.round(amount * 100)

                val existing = budgetRepository.getBudgetForMonth(userId, state.selectedMonth)
                if (existing != null) {
                    budgetRepository.updateBudget(
                        existing.copy(
                            amount = amountGrosze,
                            updatedAt = now
                        )
                    )
                } else {
                    budgetRepository.addBudget(
                        Budget(
                            userId = userId,
                            month = state.selectedMonth,
                            amount = amountGrosze,
                            updatedAt = now,
                            deletedAt = null
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "Zapisano budżet na ${formatMonthLabel(state.selectedMonth)}."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Nie udało się zapisać: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteBudget(monthKey: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            try {
                budgetRepository.deleteBudget(userId, monthKey)
                _uiState.update {
                    it.copy(message = "Usunięto budżet na ${formatMonthLabel(monthKey)}.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Nie udało się usunąć: ${e.message}")
                }
            }
        }
    }

    private fun formatMonthLabel(monthKey: String): String {
        return runCatching {
            val parts = monthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val cal = Calendar.getInstance().apply {
                clear()
                set(year, month, 1)
            }
            val locale = Locale("pl", "PL")
            val name = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
                .uppercase(locale)
            "$name $year"
        }.getOrDefault(monthKey)
    }
}