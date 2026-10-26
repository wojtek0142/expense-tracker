package com.example.expense_tracker.ui.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class ReportPeriod {
    WEEK,   // Tydzień (T)
    MONTH,  // Miesiąc (M)
    YEAR    // Rok (R)
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
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val currentPeriodTotal: Double = 3842.50,
    val previousPeriodTotal: Double = 3418.70,
    val totalCategoriesCount: Int = 5,
    val topCategoriesPercentage: Int = 80,
    val categories: List<CategoryReport> = emptyList()
)

@HiltViewModel
class ReportsViewModel @Inject constructor() : ViewModel() {

    private val sampleCategories = listOf(
        CategoryReport(
            id = 1,
            name = "Jedzenie i gastronomia",
            amount = 1306.45,
            percentage = 34,
            color = Color(0xFF004D40), // Ciemnozielony
            icon = Icons.Default.Restaurant
        ),
        CategoryReport(
            id = 2,
            name = "Mieszkanie i media",
            amount = 1075.90,
            percentage = 28,
            color = Color(0xFF795548), // Brązowy
            icon = Icons.Default.Home
        ),
        CategoryReport(
            id = 3,
            name = "Transport",
            amount = 691.65,
            percentage = 18,
            color = Color(0xFF546E7A), // Grafitowy
            icon = Icons.Default.DirectionsCar
        ),
        CategoryReport(
            id = 4,
            name = "Zakupy",
            amount = 461.10,
            percentage = 12,
            color = Color(0xFF00838F), // Cyan
            icon = Icons.Default.ShoppingBag
        ),
        CategoryReport(
            id = 5,
            name = "Inne",
            amount = 307.40,
            percentage = 8,
            color = Color(0xFF9E9E9E), // Szary
            icon = Icons.Default.MoreHoriz
        )
    )

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            categories = sampleCategories
        )
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun onPeriodSelected(period: ReportPeriod) {
        _uiState.update { currentState ->
            currentState.copy(selectedPeriod = period)
        }
    }
}