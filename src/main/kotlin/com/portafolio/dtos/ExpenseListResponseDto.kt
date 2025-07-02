package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ExpenseListResponseDto(

    @JsonProperty("expense_id")
    val expenseId: Long,

    @JsonProperty("expense_type")
    val expenseType: String,

    @JsonProperty("value")
    val value: BigDecimal,

    @JsonProperty("expense_date")
    val expenseDate: LocalDateTime,

    @JsonProperty("justification")
    val justification: String?,

    @JsonProperty("wallet_id")
    val walletId: Int?,

    @JsonProperty("username")
    val username: String

) 