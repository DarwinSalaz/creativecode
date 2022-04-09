package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class ExpenseResumeDto(

    @JsonProperty("expense_type")
    var expenseType: String,

    @JsonProperty("value")
    val value: String,

    @JsonProperty("expense_date")
    val expenseDate: String,

    @JsonProperty("justification")
    var justification: String?

)
