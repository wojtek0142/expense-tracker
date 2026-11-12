package com.example.expense_tracker.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.local.dao.CategoryTotal
import com.example.expense_tracker.data.repository.BudgetRepository
import com.example.expense_tracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import com.example.expense_tracker.data.local.datastore.SessionManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.expense_tracker.domain.model.Budget
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

enum class IconType { FOOD, TRANSPORT, SHOPPING, HOME, SUBSCRIPTION, PHONE }
data class BudgetState(
    val remaining: Double = 0.0,
    val totalBudget: Double = 0.0,
    val spent: Double = 0.0,
    val daysLeft: Int = 0,
    val dailyAverage: Double = 0.0,
    val canSpendDaily: Double = 0.0,
    val percentageUsed: Int = 0,
    val previousMonthSameDaySpent: Double = 0.0,
    val monthOverMonthChangeAmount: Double? = null,
    val monthOverMonthChangePercent: Double? = null,
    val projectedSpent: Double = 0.0,
    val projectedOverUnder: Double = 0.0,
    val projectedOverUnderPercent: Double = 0.0
)

data class MonthProgress(
    val daysPassed: Int,
    val daysInMonth: Int,
    val daysLeft: Int,
    val dailyAverage: Double,
    val canSpendDaily: Double,
    val projectedSpent: Double
)

data class UpcomingPayment(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String, // np. "25 PAŹ"
    val subtitle: String, // np. "Co miesiąc"
    val daysLeft: String, // np. "Jutro"
    val isUrgent: Boolean = false
)

data class Transaction(
    // do zmiany pod baze
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val amount: Double,
    val iconType: IconType,
    val isExpense: Boolean = true
)

data class MainScreenState(
    val userName: String = "",
    val currentDate: String = "",
    val currentMonth: String = "",
    val previousMonth: String = "",
    val budget: BudgetState = BudgetState(),
    val recentTransactions: List<Transaction> = emptyList(),
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    val topCategory: String = "",
    val topCategoryAmount: Double = 0.0,
    val topCategoryPercent: Int = 0,
    val hasOverduePayment: Boolean = true, // Dla czerwonego alertu na dole
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(MainScreenState(isLoading = true))
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()
    val monthlyExpenseCount: StateFlow<Int> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            expenseRepository.observeExpenseCountFromMonthStart(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = 0
        )

    private val monthlyExpenseAmount: StateFlow<Long> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            expenseRepository.observeExpenseAmountFromMonthStart(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = 0L
        )
    private val previousMonthSameDayAmount: StateFlow<Long> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            expenseRepository.observeExpenseAmountFromPreviousMonthSameDay(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), 0L)

    private val topCategoryFlow: StateFlow<CategoryTotal?> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            expenseRepository.observeTopCategoryFromMonthStart(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

    private val budgetFlow: StateFlow<Budget?> = _userId
        .filterNotNull()
        .flatMapLatest { userId ->
            budgetRepository.observeBudgetForMonth(userId, currentMonthKey())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

    init {
        loadData()
        updateCurrentDate()
        observeMonthlyAmount()
        observeTopCategory()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Brak zalogowanego użytkownika.")
                }
                return@launch
            }
            _userId.value = userId

            // Płatności zostają
            val payments = listOf(
                UpcomingPayment("1", "Czynsz", 2050.00, "25 PAŹ", "Co miesiąc", "Jutro", true),
                UpcomingPayment("2", "Netflix", 43.00, "27 PAŹ", "Co miesiąc", "Za 3 dni"),
                UpcomingPayment("3", "Telefon", 141.99, "29 PAŹ", "Co miesiąc", "Za 5 dni")
            )

            _uiState.update {
                it.copy(isLoading = false, upcomingPayments = payments)
            }

            // Transakcje z bazy – Flow, odświeża się automatycznie
            expenseRepository.observeRecentTransactions(userId, limit = 3)
                .catch { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Błąd ładowania wydatków: ${e.message}")
                    }
                }
                .collect { transactions ->
                    _uiState.update { it.copy(recentTransactions = transactions) }
                }
        }
    }



    private fun updateCurrentDate() {
        val calendar = Calendar.getInstance()
        val locale = Locale("pl", "PL")

        val dateFormat = SimpleDateFormat(
            "EEEE, dd MMMM",
            locale
        )

        val formattedDate = dateFormat.format(calendar.time)
            .uppercase(locale)

        // Nazwa miesiąca (np. "LISTOPAD")
        val month = calendar
            .getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
            .uppercase(locale)

        // Poprzedni miesiąc
        val previousCalendar = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val previousMonth = previousCalendar
            .getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
            .uppercase(locale)

        _uiState.update {
            it.copy(
                currentDate = formattedDate,
                currentMonth = month,
                previousMonth = previousMonth
            )
        }
    }

    private fun observeMonthlyAmount() {
        viewModelScope.launch {
            combine(
                monthlyExpenseAmount,
                previousMonthSameDayAmount,
                budgetFlow
            ) { current, previous, budget ->
                Triple(current, previous, budget)
            }.collect { (currentGrosze, previousGrosze, budget) ->
                val currentZloty = currentGrosze / 100.0
                val previousZloty = previousGrosze / 100.0

                // Budżet z bazy; brak rekordu → 0.0
                val totalBudget = budget?.amount?.div(100.0) ?: 0.0

                val progress = calculateMonthProgress(
                    spent = currentZloty,
                    totalBudget = totalBudget
                )

                // % zmiany: (current - previous) / previous * 100
                // null, gdy w poprzednim miesiącu nie było wydatków (dzielenie przez 0)
                val changePercent = if (previousZloty > 0.0) {
                    ((currentZloty - previousZloty) / previousZloty) * 100.0
                } else null

                // różnica w zł (null gdy brak danych z poprzedniego miesiąca)
                val changeAmount = if (previousZloty > 0.0) {
                    currentZloty - previousZloty
                } else null

                // Prognoza: ile przekroczy (+) lub zaoszczędzi (-) budżet
                val projectedOverUnder = progress.projectedSpent - totalBudget
                val projectedOverUnderPercent = if (totalBudget > 0.0) {
                    (projectedOverUnder / totalBudget) * 100.0
                } else 0.0

                _uiState.update { state ->
                    val budget = state.budget
                    val remaining = (totalBudget - currentZloty).coerceAtLeast(0.0)
                    val percentageUsed = if (totalBudget > 0) {
                        ((currentZloty / totalBudget) * 100).toInt().coerceIn(0, 100)
                    } else 0

                    state.copy(
                        budget = budget.copy(
                            totalBudget = totalBudget,
                            spent = currentZloty,
                            remaining = remaining,
                            percentageUsed = percentageUsed,
                            daysLeft = progress.daysLeft,
                            dailyAverage = progress.dailyAverage,
                            canSpendDaily = progress.canSpendDaily,
                            previousMonthSameDaySpent = previousZloty,
                            monthOverMonthChangeAmount = changeAmount,
                            monthOverMonthChangePercent = changePercent,
                            projectedSpent = progress.projectedSpent,
                            projectedOverUnder = projectedOverUnder,
                            projectedOverUnderPercent = projectedOverUnderPercent
                        )
                    )
                }
            }
        }
    }

    private fun observeTopCategory() {
        viewModelScope.launch {
            combine(topCategoryFlow, monthlyExpenseAmount) { top, totalGrosze ->
                top to totalGrosze
            }
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Błąd kategorii: ${e.message}") }
                }
                .collect { (top, totalGrosze) ->
                    val totalZloty = totalGrosze / 100.0

                    if (top == null) {
                        _uiState.update {
                            it.copy(
                                topCategory = "Brak wydatków",
                                topCategoryAmount = 0.0,
                                topCategoryPercent = 0
                            )
                        }
                    } else {
                        val topZloty = top.totalAmount / 100.0
                        val percent = if (totalZloty > 0.0) {
                            ((topZloty / totalZloty) * 100).toInt()
                        } else 0

                        _uiState.update {
                            it.copy(
                                topCategory = top.categoryName,
                                topCategoryAmount = topZloty,
                                topCategoryPercent = percent
                            )
                        }
                    }
                }
        }
    }
}

private fun currentMonthKey(): String {
    val cal = Calendar.getInstance()
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%04d-%02d", y, m)
}

fun calculateMonthProgress(
    spent: Double,
    totalBudget: Double,
    calendar: Calendar = Calendar.getInstance()
): MonthProgress {
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val daysPassed = today
    val daysLeft = (daysInMonth - today).coerceAtLeast(1)
    val remaining = (totalBudget - spent).coerceAtLeast(0.0)

    val dailyAverage = if (daysPassed > 0) spent / daysPassed else 0.0
    val canSpendDaily = remaining / daysLeft
    val projectedSpent = dailyAverage * daysInMonth

    return MonthProgress(
        daysPassed = daysPassed,
        daysInMonth = daysInMonth,
        daysLeft = daysLeft,
        dailyAverage = dailyAverage,
        canSpendDaily = canSpendDaily,
        projectedSpent = projectedSpent
    )
}
