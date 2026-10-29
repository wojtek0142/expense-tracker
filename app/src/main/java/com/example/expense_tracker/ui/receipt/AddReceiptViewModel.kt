package com.example.expense_tracker.ui.receipt

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AddReceiptUiState(
    val isProcessing: Boolean = false,
    val message: String = "Zrób zdjęcie aparatem lub wybierz z galerii.",
    val error: String? = null
)

@HiltViewModel
class AddReceiptViewModel @Inject constructor(
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddReceiptUiState())
    val uiState: StateFlow<AddReceiptUiState> = _uiState.asStateFlow()

    fun onImageCaptured(uri: Uri?) {
        if (uri == null) return
        // TODO: Tutaj przekażemy Uri do modułu OCR (ML Kit Text Recognition)
    }
}