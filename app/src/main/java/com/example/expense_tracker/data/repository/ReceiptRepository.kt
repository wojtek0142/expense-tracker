package com.example.expense_tracker.data.repository

import com.example.expense_tracker.data.model.ReceiptData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor() {

    /**
     * Zapisuje do bazy danych przetworzone dane paragonu (wydatek, pozycje, kategoria).
     * Zdjęcie NIE jest wysyłane – przetwarzanie OCR odbywa się na telefonie.
     */
    suspend fun saveReceipt(receipt: ReceiptData): Boolean {
        // TODO: Tutaj zaimplementujemy zapis przetworzonych danych do bazy danych / serwera API
        return true
    }
}