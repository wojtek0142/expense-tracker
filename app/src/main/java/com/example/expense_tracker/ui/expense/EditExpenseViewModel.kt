package com.example.expense_tracker.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.CategoryRepository
import com.example.expense_tracker.data.repository.ExpenseRepository
import com.example.expense_tracker.data.repository.ProductRepository
import com.example.expense_tracker.domain.model.Category
import com.example.expense_tracker.domain.model.CategoryOption
import com.example.expense_tracker.domain.model.Expense
import com.example.expense_tracker.domain.model.Product
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

data class EditExpenseUiState(
    val title: String = "",
    val amount: String = "",          // kwota łączna, np. "43,00"
    val quantity: Int = 1,
    val date: Long = System.currentTimeMillis(),
    val categoryId: String? = null,
    val categoryName: String = "",
    val availableCategories: List<CategoryOption> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false,  // zapisano lub usunięto → wróć
    val showDeleteConfirm: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Argument z trasy "edit_expense/{expenseId}"
    private val expenseId: String = checkNotNull(savedStateHandle["expenseId"])

    private val _uiState = MutableStateFlow(EditExpenseUiState())
    val uiState: StateFlow<EditExpenseUiState> = _uiState.asStateFlow()

    private var original: Expense? = null

    init {
        loadExpense()
        observeCategories()
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }

    fun onAmountChange(value: String) {
        if (value.matches(Regex("^\\d*[.,]?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = value, errorMessage = null) }
        }
    }

    fun onQuantityChange(value: Int) = _uiState.update { it.copy(quantity = value.coerceAtLeast(1)) }

    fun onDateChange(millis: Long) = _uiState.update { it.copy(date = millis) }

    fun onCategorySelected(option: CategoryOption) =
        _uiState.update { it.copy(categoryId = option.id, categoryName = option.name, errorMessage = null) }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun cancelDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun save() {
        val state = _uiState.value
        val expense = original ?: return
        val title = state.title.trim()
        val amountValue = state.amount.replace(',', '.').toDoubleOrNull()

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Podaj nazwę wydatku.") }
            return
        }
        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Podaj poprawną kwotę większą od zera.") }
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val now = System.currentTimeMillis()
                val categoryId = resolveCategoryId(userId, state, now)
                val productId = resolveProductId(userId, title, categoryId, now)

                expenseRepository.updateExpense(
                    expense.copy(
                        productId = productId,
                        quantity = state.quantity.toDouble(),
                        amount = Math.round(amountValue * 100),
                        date = state.date,
                        updatedAt = now
                    )
                )
                _uiState.update { it.copy(isSaving = false, isFinished = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Nie udało się zapisać: ${e.message}")
                }
            }
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            try {
                expenseRepository.deleteExpense(expenseId, userId)
                _uiState.update { it.copy(showDeleteConfirm = false, isFinished = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(showDeleteConfirm = false, errorMessage = "Nie udało się usunąć: ${e.message}")
                }
            }
        }
    }

    private fun loadExpense() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Brak zalogowanego użytkownika.") }
                return@launch
            }

            val expense = expenseRepository.getExpenseById(expenseId, userId)
            val details = expenseRepository.getExpenseDetailsById(expenseId, userId)
            if (expense == null || details == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Nie znaleziono wydatku.") }
                return@launch
            }
            original = expense

            val product = productRepository.getProductById(expense.productId, userId)

            _uiState.update {
                it.copy(
                    title = details.productName,
                    amount = "%.2f".format(Locale("pl", "PL"), details.amount / 100.0),
                    quantity = details.quantity.toInt().coerceAtLeast(1),
                    date = details.date,
                    categoryId = product?.categoryId,
                    categoryName = details.categoryName,
                    isLoading = false
                )
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            categoryRepository.observeCategoriesOrderedByUsage(userId)
                .catch { }
                .collect { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                }
        }
    }

    private suspend fun resolveCategoryId(userId: String, state: EditExpenseUiState, now: Long): String {
        state.categoryId?.let { return it }

        val name = state.categoryName.ifBlank { "Bez kategorii" }
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

    // Tak jak w AddExpenseViewModel: produkt po nazwie, tworzony gdy nie istnieje
    private suspend fun resolveProductId(userId: String, title: String, categoryId: String, now: Long): String {
        val nameKey = title.lowercase(Locale.getDefault())

        val existing = productRepository.findProductByName(userId, nameKey)
        if (existing != null) {
            if (existing.categoryId != categoryId) {
                productRepository.updateProduct(existing.copy(categoryId = categoryId, updatedAt = now))
            }
            return existing.id
        }

        val product = Product(
            id = UUID.randomUUID().toString(),
            name = title,
            nameKey = nameKey,
            userId = userId,
            categoryId = categoryId,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        productRepository.addProduct(product)
        return product.id
    }
}