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
