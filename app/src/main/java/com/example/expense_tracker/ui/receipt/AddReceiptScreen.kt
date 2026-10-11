package com.example.expense_tracker.ui.receipt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun AddReceiptScreen(
    viewModel: AddReceiptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onReceiptSelected(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Środek ekranu – informacja
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Twoje Wydatki",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tutaj pojawi się lista Twoich ostatnich wydatków.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Dół ekranu – status + przycisk
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.error != null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )

            if (uiState.isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!uiState.isUploading) {
                        photoPicker.launch("image/*")
                    }
                },
                enabled = !uiState.isUploading
            ) {
                Text(
                    text = if (uiState.isUploading) "Wysyłanie..." else "Dodaj paragon"
                )
            }
        }
    }
}