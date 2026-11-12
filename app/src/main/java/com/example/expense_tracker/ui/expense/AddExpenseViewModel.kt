
package com.example.expense_tracker.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.ExpenseRepository
import com.example.expense_tracker.data.repository.ProductRepository
import com.example.expense_tracker.data.repository.CategoryRepository
import com.example.expense_tracker.domain.model.Category
import com.example.expense_tracker.domain.model.Expense
import com.example.expense_tracker.domain.model.Product
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.catch
import com.example.expense_tracker.domain.model.CategoryOption
// Jeden wydatek na liście podglądowej
data class PendingExpense(
    val id: String = UUID.randomUUID().toString(),
    val amount: String,
    val quantity: Int = 1,
    val title: String,
    val date: String,
    val category: String,
    val productId: String? = null,   // null → utwórz produkt
    val categoryId: String? = null   // null → utwórz/użyj "Bez kategorii"
) {
    val totalAmount: Double
        get() = (amount.replace(',', '.').toDoubleOrNull() ?: 0.0) * quantity
}

// Stan ekranu
data class AddExpenseUiState(
    val amount: String = "",
    val title: String = "",
    val quantity: Int = 1,
    val date: String = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        .format(java.util.Date()),
    val categoryId: String? = null,          // <-- NOWE: wybrana kategoria z bazy
    val availableCategories: List<CategoryOption> = emptyList(), // <-- NOWE
    val category: String = "",
    val pendingExpenses: List<PendingExpense> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {


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

    fun onCategorySelected(option: CategoryOption) {
        _uiState.update {
            it.copy(
                category = option.name,
                categoryId = option.id,   // null → nowa kategoria
                errorMessage = null
            )
        }
    }

    fun onCategoryTextChange(newCategory: String) {
        _uiState.update {
            it.copy(
                category = newCategory,
                categoryId = null,   // wpisana nazwa = potencjalnie nowa kategoria
                errorMessage = null
            )
        }
    }

    init {
        observeCategories()
    }

    // Dodaje bieżący formularz do listy podglądowej.
    // Nie zapisuje jeszcze danych do bazy.
    fun addExpenseToList() {
        val state = _uiState.value

        val title = state.title.trim()
        val normalizedAmount = state.amount.replace(',', '.')
        val amountValue = normalizedAmount.toDoubleOrNull()

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Podaj nazwę wydatku.") }
            return
        }

        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Podaj poprawną kwotę większą od zera.") }
            return
        }

        val expense = PendingExpense(
            amount = state.amount,          // String — zachowujemy format z przecinkiem
            quantity = state.quantity,
            title = title,
            date = state.date,
            category = state.category.ifBlank { "Bez kategorii" },
            categoryId = state.categoryId
        )

        _uiState.update {
            it.copy(
                pendingExpenses = it.pendingExpenses + expense,
                amount = "",
                title = "",
                quantity = 1,
                errorMessage = null
            )
        }
    }

    fun onQuantityChange(newQuantity: Int) {
        _uiState.update { it.copy(quantity = newQuantity.coerceAtLeast(1)) }
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
            _uiState.update { it.copy(errorMessage = "Dodaj przynajmniej jeden wydatek.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Brak zalogowanego użytkownika.")
                }
                return@launch
            }

            try {
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                val expenses = state.pendingExpenses.map { pending ->
                    val productId = resolveProductId(userId, pending, now)

                    Expense(
                        id = pending.id,
                        productId = productId,
                        userId = userId,
                        quantity = pending.quantity.toDouble(),
                        amount = Math.round(pending.totalAmount * 100), // grosze
                        date = runCatching {
                            dateFormat.parse(pending.date)?.time
                        }.getOrNull() ?: now,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null
                    )
                }

                expenseRepository.addExpenses(expenses)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        pendingExpenses = emptyList(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nie udało się zapisać wydatków: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun resolveProductId(
        userId: String,
        pending: PendingExpense,
        now: Long
    ): String {
        pending.productId?.let { return it }

        val nameKey = pending.title.lowercase(Locale.getDefault())

        productRepository.findProductByName(userId, nameKey)?.let { return it.id }

        val categoryId = resolveCategoryId(userId, pending, now)

        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = pending.title,
            nameKey = nameKey,
            userId = userId,
            categoryId = categoryId,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        productRepository.addProduct(newProduct)
        return newProduct.id
    }

    private suspend fun resolveCategoryId(
        userId: String,
        pending: PendingExpense,
        now: Long
    ): String {
        // Jeśli użytkownik wybrał istniejącą kategorię — użyj jej
        pending.categoryId?.let { return it }

        val name = pending.category.ifBlank { "Bez kategorii" }

        categoryRepository.findBuiltinCategoryByName(name)?.let { return it.id }
        categoryRepository.findUserCategoryByName(userId, name)?.let { return it.id }

        // Nowa kategoria — dodaj do bazy
        val newCategory = Category(
            id = UUID.randomUUID().toString(),
            name = name,
            isBuiltin = false,
            userId = userId,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        categoryRepository.addCategory(newCategory)
        return newCategory.id
    }

    private fun observeCategories() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            categoryRepository.observeCategoriesOrderedByUsage(userId)
                .catch { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Błąd ładowania kategorii: ${e.message}")
                    }
                }
                .collect { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                }
        }
    }
}

