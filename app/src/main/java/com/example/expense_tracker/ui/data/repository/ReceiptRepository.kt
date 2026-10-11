package com.example.expense_tracker.ui.data.repository

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val contentResolver: ContentResolver
) {
    private val serverUrl = "http://10.0.2.2:8000"

    suspend fun uploadReceipt(uri: Uri): Int = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val bytes = contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw IOException("Nie udało się odczytać zdjęcia.")

        if (bytes.size > 10 * 1024 * 1024) {
            throw IOException("Zdjęcie jest za duże. Maksymalny rozmiar to 10 MB.")
        }

        val fileBody = bytes.toRequestBody(mimeType.toMediaType())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "receipt.$extension", fileBody)
            .build()

        val request = Request.Builder()
            .url("$serverUrl/receipts")
            .post(multipartBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("Serwer zwrócił błąd ${response.code}: $responseText")
            }

            return@withContext JSONObject(responseText).getInt("id")
        }
    }
}