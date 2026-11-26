package com.example.expense_tracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.CategoryRepository
import com.example.expense_tracker.data.repository.RecurringPaymentRepository
import com.example.expense_tracker.domain.model.Category
import com.example.expense_tracker.domain.model.CategoryOption
import com.example.expense_tracker.domain.model.RecurringFrequency
import com.example.expense_tracker.domain.model.RecurringPayment
import com.example.expense_tracker.domain.model.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

// Formularz dodawania / edycji (null w UiState = dialog zamknięty)
data class RecurringEditorState(
    val editingId: String? = null,
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val categoryName: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val nextDueDate: Long = startOfDay(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean get() = editingId != null
}

data class RecurringPaymentsUiState(
    val payments: List<RecurringPayment> = emptyList(),
    val categories: List<CategoryOption> = emptyList(),
    val monthlyTotal: Double = 0.0,          // zł
    val editor: RecurringEditorState? = null,
    val pendingDelete: RecurringPayment? = null,
    val payingPaymentIds: Set<String> = emptySet(), // trwa dodawanie do wydatków
    val isLoading: Boolean = true,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class RecurringPaymentsViewModel @Inject constructor(
    private val recurringRepository: RecurringPaymentRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringPaymentsUiState())
    val uiState: StateFlow<RecurringPaymentsUiState> = _uiState.asStateFlow()

    init {
        observePayments()
        observeCategories()
    }

    // ---------- Edytor ----------

    fun startAdd() {
        _uiState.update { it.copy(editor = RecurringEditorState(), message = null) }
    }

    fun startEdit(payment: RecurringPayment) {
        _uiState.update {
            it.copy(
                message = null,
                editor = RecurringEditorState(
                    editingId = payment.id,
                    name = payment.name,
                    amount = "%.2f".format(Locale("pl", "PL"), payment.amount / 100.0),
                    categoryId = payment.categoryId,
                    categoryName = payment.categoryName ?: "",
                    frequency = payment.frequency,
                    nextDueDate = payment.nextDueDate
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun onNameChange(value: String) = updateEditor { it.copy(name = value, errorMessage = null) }

    fun onAmountChange(value: String) {
        if (value.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
            updateEditor { it.copy(amount = value, errorMessage = null) }
        }
    }

    fun onCategorySelected(option: CategoryOption) =
        updateEditor { it.copy(categoryId = option.id, categoryName = option.name, errorMessage = null) }

    fun onFrequencySelected(frequency: RecurringFrequency) =
        updateEditor { it.copy(frequency = frequency) }

    fun onNextDueDateSelected(millis: Long) =
        updateEditor { it.copy(nextDueDate = startOfDay(millis)) }

    fun save() {
        val editor = _uiState.value.editor ?: return
        val name = editor.name.trim()
        val amountValue = editor.amount.replace(',', '.').toDoubleOrNull()

        if (name.isBlank()) {
            updateEditor { it.copy(errorMessage = "Podaj nazwę opłaty.") }
            return
        }
        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0.0) {
            updateEditor { it.copy(errorMessage = "Podaj poprawną kwotę większą od zera.") }
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                updateEditor { it.copy(errorMessage = "Brak zalogowanego użytkownika.") }
                return@launch
            }

            updateEditor { it.copy(isSaving = true, errorMessage = null) }

            try {
                val now = System.currentTimeMillis()
                val categoryId = resolveCategoryId(userId, editor, now)
                val amountGrosze = Math.round(amountValue * 100)

                if (editor.editingId == null) {
                    recurringRepository.addRecurringPayment(
                        RecurringPayment(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            categoryId = categoryId,
                            name = name,
                            amount = amountGrosze,
                            frequency = editor.frequency,
                            startDate = editor.nextDueDate,
                            nextDueDate = editor.nextDueDate,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    val existing = recurringRepository.getRecurringPaymentById(editor.editingId, userId)
                        ?: throw IllegalStateException("Opłata nie istnieje.")

                    // Zmiana terminu lub częstotliwości = nowa "kotwica" harmonogramu
                    val scheduleChanged = existing.nextDueDate != editor.nextDueDate ||
                            existing.frequency != editor.frequency

                    recurringRepository.updateRecurringPayment(
                        existing.copy(
                            name = name,
                            amount = amountGrosze,
                            categoryId = categoryId,
                            frequency = editor.frequency,
                            nextDueDate = editor.nextDueDate,
                            startDate = if (scheduleChanged) editor.nextDueDate else existing.startDate
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        editor = null,
                        message = if (editor.isEditing) "Zapisano zmiany" else "Dodano \"$name\""
                    )
                }
            } catch (e: Exception) {
                updateEditor {
                    it.copy(isSaving = false, errorMessage = "Nie udało się zapisać: ${e.message}")
                }
            }
        }
    }

    // ---------- Płacenie ("+") ----------

    /** Tak jak na ekranie głównym: dodaje opłatę do wydatków i przesuwa termin o jeden okres. */
    fun payRecurringPayment(paymentId: String) {
        if (paymentId in _uiState.value.payingPaymentIds) return

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(payingPaymentIds = it.payingPaymentIds + paymentId) }

            try {
                val name = recurringRepository.registerPayment(paymentId, userId)
                _uiState.update {
                    it.copy(
                        message = if (name != null) "Dodano \"$name\" do wydatków"
                        else "Nie znaleziono opłaty"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Nie udało się dodać wydatku: ${e.message}") }
            } finally {
                _uiState.update { it.copy(payingPaymentIds = it.payingPaymentIds - paymentId) }
            }
        }
    }

    // ---------- Usuwanie ----------

    fun requestDelete(payment: RecurringPayment) {
        _uiState.update { it.copy(pendingDelete = payment) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val payment = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            try {
                recurringRepository.deleteRecurringPayment(payment.id, userId)
                _uiState.update {
                    it.copy(pendingDelete = null, editor = null, message = "Usunięto \"${payment.name}\"")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(pendingDelete = null, errorMessage = "Nie udało się usunąć: ${e.message}")
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    // ---------- Prywatne ----------

    private fun updateEditor(transform: (RecurringEditorState) -> RecurringEditorState) {
        _uiState.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }
    }

    private suspend fun resolveCategoryId(
        userId: String,
        editor: RecurringEditorState,
        now: Long
    ): String {
        editor.categoryId?.let { return it }

        val name = editor.categoryName.ifBlank { "Bez kategorii" }
        categoryRepository.findBuiltinCategoryByName(name)?.let { return it.id }
        categoryRepository.findUserCategoryByName(userId, name)?.let { return it.id }

        val newCategory = Category(
            id = UUID.randomUUID().toString(),
            name = name,
            isBuiltin = false,
            userId = userId,
            createdAt = now,
            updatedAt = now
        )
        categoryRepository.addCategory(newCategory)
        return newCategory.id
    }

    private fun observePayments() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Brak zalogowanego użytkownika.") }
                return@launch
            }
            recurringRepository.observeRecurringPayments(userId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Błąd ładowania opłat: ${e.message}")
                    }
                }
                .collect { payments ->
                    _uiState.update {
                        it.copy(
                            payments = payments,
                            monthlyTotal = payments.sumOf { p -> p.monthlyCost } / 100.0,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            categoryRepository.observeCategoriesOrderedByUsage(userId)
                .catch { /* lista kategorii nie jest krytyczna */ }
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }
}