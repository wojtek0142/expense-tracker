package com.example.expense_tracker.ui.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.datastore.SessionManager
import com.example.expense_tracker.data.repository.CategoryRepository
import com.example.expense_tracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class TimeFilter(val label: String) {
    LAST_30_DAYS("Ostatnie 30 dni"),
    LAST_7_DAYS("Ostatnie 7 dni"),
    LAST_90_DAYS("Ostatnie 90 dni"),
    CURRENT_YEAR("Bieżący rok"),
    CUSTOM_RANGE("Niestandardowy zakres")
}

data class CategoryReport(
    val id: Long,
    val name: String,
    val amount: Double,
    val percentage: Int,
    val color: Color,
    val icon: ImageVector
)

data class ReportsUiState(
    val selectedTimeFilter: TimeFilter = TimeFilter.LAST_30_DAYS,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val currentPeriodTotal: Double = 0.0,
    val previousPeriodTotal: Double = 0.0,
    val totalCategoriesCount: Int = 0,
    val topCategoriesPercentage: Int = 0,
    val categories: List<CategoryReport> = emptyList(),
    val isLoading: Boolean = false
) {
    val periodLabel: String
        get() = if (selectedTimeFilter == TimeFilter.CUSTOM_RANGE && customStartDate != null && customEndDate != null) {
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            "${formatter.format(Date(customStartDate))} - ${formatter.format(Date(customEndDate))}"
        } else {
            selectedTimeFilter.label.uppercase()
        }
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadDataForFilter(_uiState.value.selectedTimeFilter, null, null)
    }

    fun onTimeFilterSelected(filter: TimeFilter) {
        if (filter == TimeFilter.CUSTOM_RANGE) return

        _uiState.update {
            it.copy(
                selectedTimeFilter = filter,
                customStartDate = null,
                customEndDate = null
            )
        }
        loadDataForFilter(filter, null, null)
    }

    fun onCustomDateRangeSelected(startDateMillis: Long?, endDateMillis: Long?) {
        if (startDateMillis == null || endDateMillis == null) return

        _uiState.update {
            it.copy(
                selectedTimeFilter = TimeFilter.CUSTOM_RANGE,
                customStartDate = startDateMillis,
                customEndDate = endDateMillis
            )
        }
        loadDataForFilter(TimeFilter.CUSTOM_RANGE, startDateMillis, endDateMillis)
    }

    private fun loadDataForFilter(filter: TimeFilter, customStart: Long?, customEnd: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = sessionManager.getUserId() ?: "default_user"
            val now = System.currentTimeMillis()

            val (startDate, endDate) = when (filter) {
                TimeFilter.LAST_7_DAYS -> (now - 7L * 24 * 60 * 60 * 1000) to now
                TimeFilter.LAST_30_DAYS -> (now - 30L * 24 * 60 * 60 * 1000) to now
                TimeFilter.LAST_90_DAYS -> (now - 90L * 24 * 60 * 60 * 1000) to now
                TimeFilter.CURRENT_YEAR -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    cal.timeInMillis to now
                }
                TimeFilter.CUSTOM_RANGE -> {
                    (customStart ?: (now - 30L * 24 * 60 * 60 * 1000)) to (customEnd ?: now)
                }
            }

            // Pobieranie danych z bazy Room
            val currentTotalGrosze = expenseRepository.getTotalAmountForPeriod(userId, startDate, endDate)
            val periodDuration = (endDate - startDate).coerceAtLeast(1)
            val previousStartDate = startDate - periodDuration
            val previousEndDate = startDate
            val previousTotalGrosze = expenseRepository.getTotalAmountForPeriod(userId, previousStartDate, previousEndDate)

            val categoryTotals = expenseRepository.getExpensesPerCategoryForPeriod(userId, startDate, endDate)

            val currentTotalPLN = currentTotalGrosze / 100.0
            val previousTotalPLN = previousTotalGrosze / 100.0

            val mappedCategories = categoryTotals.mapIndexed { index, item ->
                val amountPLN = item.totalAmount / 100.0
                val pct = if (currentTotalGrosze > 0) ((item.totalAmount * 100) / currentTotalGrosze).toInt() else 0

                CategoryReport(
                    id = (index + 1).toLong(),
                    name = item.categoryName,
                    amount = amountPLN,
                    percentage = pct,
                    color = getCategoryColor(item.categoryName, index),
                    icon = getCategoryIcon(item.categoryName)
                )
            }

            val top3Pct = mappedCategories.take(3).sumOf { it.percentage }

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    currentPeriodTotal = currentTotalPLN,
                    previousPeriodTotal = previousTotalPLN,
                    totalCategoriesCount = mappedCategories.size,
                    topCategoriesPercentage = top3Pct,
                    categories = mappedCategories
                )
            }
        }
    }

    private fun getCategoryColor(name: String, index: Int): Color {
        val lowercase = name.lowercase()
        return when {
            "jedzen" in lowercase || "gastronomia" in lowercase -> Color(0xFF004D40)
            "mieszkan" in lowercase || "media" in lowercase -> Color(0xFF795548)
            "transport" in lowercase || "paliwo" in lowercase -> Color(0xFF546E7A)
            "zakup" in lowercase || "sklep" in lowercase -> Color(0xFF00838F)
            else -> {
                val palette = listOf(
                    Color(0xFF004D40), Color(0xFF795548), Color(0xFF546E7A),
                    Color(0xFF00838F), Color(0xFF673AB7), Color(0xFFE91E63)
                )
                palette[index % palette.size]
            }
        }
    }

    private fun getCategoryIcon(name: String): ImageVector {
        val lowercase = name.lowercase()
        return when {
            "jedzen" in lowercase || "gastronomia" in lowercase -> Icons.Default.Restaurant
            "mieszkan" in lowercase || "media" in lowercase -> Icons.Default.Home
            "transport" in lowercase || "paliwo" in lowercase -> Icons.Default.DirectionsCar
            "zakup" in lowercase || "sklep" in lowercase -> Icons.Default.ShoppingBag
            else -> Icons.Default.Category
        }
    }
}