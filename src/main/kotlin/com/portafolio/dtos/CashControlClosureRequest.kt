package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import javax.validation.constraints.NotNull

data class CashControlClosureRequest(

    @field:NotNull
    @JsonProperty("cash_control_id")
    val cashControlId: Long,

    @field:NotNull
    @JsonProperty("commission")
    val commission: BigDecimal,

    @field:NotNull
    @JsonProperty("closure_value_received")
    val closureValueReceived: BigDecimal,

    @JsonProperty("closure_notes")
    val closureNotes: String? = null

)
