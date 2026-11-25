package com.example.dompetmu.model

data class TransactionItem(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val date: String,
    val note: String,
    val type: String // "INCOME" or "EXPENSE"
)
