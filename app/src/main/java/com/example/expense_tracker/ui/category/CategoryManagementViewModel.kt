package com.example.expense_tracker.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.CategoryRepository
import com.example.expense_tracker.domain.model.Category
import com.example.expense_tracker.domain.model.CategoryOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryManagementUiState(
    val newCategoryName: String = "",
    val categories: List<CategoryOption> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
    }

    fun onCategoryNameChanged(newName: String) {
        _uiState.update {
            it.copy(
                newCategoryName = newName,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun addCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Wpisz nazwę kategorii.") }
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(errorMessage = "Brak zalogowanego użytkownika.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val builtin = categoryRepository.findBuiltinCategoryByName(name)
                val userCat = categoryRepository.findUserCategoryByName(userId, name)

                if (builtin != null || userCat != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Kategoria o tej nazwie już istnieje."
                        )
                    }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val newCat = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    isBuiltin = false,
                    userId = userId,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null
                )

                categoryRepository.addCategory(newCat)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        newCategoryName = "",
                        successMessage = "Dodano kategorię \"$name\"",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nie udało się dodać kategorii: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch

            try {
                categoryRepository.deleteCategory(categoryId, userId)
                _uiState.update {
                    it.copy(
                        successMessage = "Usunięto kategorię",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Nie udało się usunąć kategorii: ${e.message}")
                }
            }
        }
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
                    _uiState.update { it.copy(categories = categories) }
                }
        }
    }
}
