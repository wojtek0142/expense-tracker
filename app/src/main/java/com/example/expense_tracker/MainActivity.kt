package com.example.expense_tracker

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.example.expense_tracker.ui.theme.ExpensetrackerTheme

class MainActivity : ComponentActivity() {

    private val serverUrl = "http://10.0.2.2:8000"

    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpensetrackerTheme {
                var isUploading by remember { mutableStateOf(false) }
                var message by remember {
                    mutableStateOf("Wybierz zdjęcie paragonu.")
                }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->

                    if (uri != null && !isUploading) {
                        isUploading = true
                        message = "Wysyłanie zdjęcia..."

                        Thread {
                            try {
                                val receiptId = uploadReceipt(uri)

                                runOnUiThread {
                                    message =
                                        "Zdjęcie zapisane! ID paragonu: $receiptId"
                                    isUploading = false
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    message =
                                        "Błąd wysyłania: ${e.message ?: "Nieznany błąd"}"
                                    isUploading = false
                                }
                            }
                        }.start()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    ReceiptHomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isUploading = isUploading,
                        message = message,
                        onAddReceiptClick = {
                            if (!isUploading) {
                                photoPicker.launch("image/*")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun uploadReceipt(uri: Uri): Int {
        val resolver = contentResolver

        val mimeType = resolver.getType(uri) ?: "image/jpeg"

        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val bytes = resolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw IOException("Nie udało się odczytać zdjęcia.")

        // Poprawiono warunek sprawdzający rozmiar (10 MB)
        if (bytes.size > 10 * 1024 * 1024) {
            throw IOException("Zdjęcie jest za duże. Maksymalny rozmiar to 10 MB.")
        }

        val fileBody = bytes.toRequestBody(mimeType.toMediaType())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "receipt.$extension",
                fileBody
            )
            .build()

        val request = Request.Builder()
            .url("$serverUrl/receipts")
            .post(multipartBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(
                    "Serwer zwrócił błąd ${response.code}: $responseText"
                )
            }

            return JSONObject(responseText).getInt("id")
        }
    }
}

@Composable
private fun ReceiptHomeScreen(
    modifier: Modifier = Modifier,
    isUploading: Boolean,
    message: String,
    onAddReceiptClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
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

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End // Wyrównuje tekst i przycisk do prawej strony
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )

            if (isUploading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddReceiptClick,
                enabled = !isUploading
            ) {
                Text(
                    text = if (isUploading) "Wysyłanie..." else "Dodaj paragon"
                )
            }
        }
    }
}