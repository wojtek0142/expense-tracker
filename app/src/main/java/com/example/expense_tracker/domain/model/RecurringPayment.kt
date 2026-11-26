package com.example.expense_tracker.domain.model

import java.util.Calendar

enum class RecurringFrequency(val label: String, val monthsFactor: Double) {
    WEEKLY("Co tydzień", 52.0 / 12.0),
    MONTHLY("Co miesiąc", 1.0),
    QUARTERLY("Co kwartał", 1.0 / 3.0),
    YEARLY("Co rok", 1.0 / 12.0);

    /** Dodaje [times] okresów do kalendarza. */
    fun addTo(calendar: Calendar, times: Int) {
        when (this) {
            WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, times)
            MONTHLY -> calendar.add(Calendar.MONTH, times)
            QUARTERLY -> calendar.add(Calendar.MONTH, 3 * times)
            YEARLY -> calendar.add(Calendar.YEAR, times)
        }
    }

    companion object {
        fun fromKey(key: String): RecurringFrequency =
            values().firstOrNull { it.name == key } ?: MONTHLY
    }
}

data class RecurringPayment(
    val id: String,
    val userId: String,
    val categoryId: String,
    val categoryName: String? = null,
    val name: String,
    val amount: Long,          // grosze
    val frequency: RecurringFrequency,
    val startDate: Long,       // ms, początek dnia
    val nextDueDate: Long,     // ms, początek dnia
    val lastPaidDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
) {
    /** Przybliżony koszt miesięczny w groszach. */
    val monthlyCost: Double
        get() = amount * frequency.monthsFactor
}

/**
 * Kolejny termin po [currentDueDate], liczony zawsze od [startDate],
 * dzięki czemu np. płatność 31-go nie "ucieka" na 28-go po lutym.
 */
fun nextDueDateAfter(
    startDate: Long,
    currentDueDate: Long,
    frequency: RecurringFrequency
): Long {
    var k = 1
    while (true) {
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        frequency.addTo(cal, k)
        if (cal.timeInMillis > currentDueDate || k >= 10_000) return cal.timeInMillis
        k++
    }
}

/** Początek dnia (lokalna północ) dla podanego czasu. */
fun startOfDay(millis: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis