package com.example.expense_tracker.data.model

import java.util.Date

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String? = null
)

data class Product(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0
)

data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Date = Date(),
    val categoryId: Long? = null,
    val receiptId: Long? = null
)

data class ReceiptData(
    val id: Long = 0,
    val storeName: String? = null,
    val totalAmount: Double = 0.0,
    val date: Date = Date(),
    val products: List<Product> = emptyList(),
    val categoryId: Long? = null
)