package com.example.expense_tracker.ui.reports

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(monthTotal = 2340.50))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
}