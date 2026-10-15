package com.example.expense_tracker.ui.receipt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddReceiptUiState(
    val isUploading: Boolean = false,
    val message: String = "Wybierz zdjęcie paragonu.",
    val uploadedReceiptId: Int? = null,
    val error: String? = null
)

@HiltViewModel
class AddReceiptViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddReceiptUiState())
    val uiState: StateFlow<AddReceiptUiState> = _uiState.asStateFlow()

    fun onReceiptSelected(uri: Uri?) {
        if (uri == null || _uiState.value.isUploading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    message = "Wysyłanie zdjęcia...",
                    error = null,
                    uploadedReceiptId = null
                )
            }

            try {
                val receiptId = receiptRepository.uploadReceipt(uri)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        message = "Zdjęcie zapisane! ID paragonu: $receiptId",
                        uploadedReceiptId = receiptId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        message = "Błąd wysyłania: ${e.message ?: "Nieznany błąd"}",
                        error = e.message
                    )
                }
            }
        }
    }
}