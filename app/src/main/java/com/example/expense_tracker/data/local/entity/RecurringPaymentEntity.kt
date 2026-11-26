package com.example.expense_tracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Opłata cykliczna (subskrypcja, czynsz, abonament itp.).
 *
 * Po kliknięciu "+" na ekranie głównym na jej podstawie tworzony jest zwykły
 * wydatek (expenses), a [next_due_date] przesuwa się o jeden okres.
 */
@Entity(
    tableName = "recurring_payments",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id"),
        Index("category_id"),
        Index("next_due_date")
    ]
)
data class RecurringPaymentEntity(
    @PrimaryKey
    val id: String, // UUID opłaty

    val user_id: String,
    val category_id: String,

    val name: String,

    // Kwota w groszach
    val amount: Long,

    // WEEKLY / MONTHLY / QUARTERLY / YEARLY (RecurringFrequency.name)
    val frequency: String,

    // Data pierwszej płatności – "kotwica" harmonogramu (ms, początek dnia)
    val start_date: Long,

    // Termin najbliższej płatności (ms, początek dnia)
    val next_due_date: Long,

    // Kiedy ostatnio opłatę dodano do wydatków
    val last_paid_date: Long? = null,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,

    val serverVersion: Int = 0,
    val isSynced: Boolean = false
)

/** Wynik zapytania: opłata + nazwa kategorii (null gdy kategorię usunięto). */
data class RecurringPaymentWithCategoryName(
    @Embedded
    val payment: RecurringPaymentEntity,
    val categoryName: String?
)