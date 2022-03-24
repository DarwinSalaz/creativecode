package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

class ExpenseDto (

    @JsonProperty("application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("expense_type")
    var expenseType: String,

    @JsonProperty("value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("expense_date")
    val expenseDate: LocalDateTime,

    @JsonProperty("justification")
    var justification: String?,

    @JsonProperty("wallet_id")
    var walletId: Int?

)