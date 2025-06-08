package com.portafolio.dtos

import java.math.BigDecimal
import java.time.LocalDateTime

data class WalletTransactionSummary(
    val type: String, // "income" o "expense"
    val date: LocalDateTime,
    val category: String,
    val value: BigDecimal,
    val justification: String?
)

data class WalletResumeResponse(
    val walletName: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val incomes: List<WalletTransactionSummary>,
    val expenses: List<WalletTransactionSummary>,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val finalBalance: BigDecimal
)
