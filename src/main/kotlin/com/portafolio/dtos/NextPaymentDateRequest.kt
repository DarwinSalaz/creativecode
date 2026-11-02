package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class NextPaymentDateRequest(
    @field:NotNull
    @JsonProperty("service_id")
    val serviceId: Long,
    @field:NotNull
    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDate
)

data class NextPaymentDateResponse(
    @JsonProperty("service_id")
    val serviceId: Long,
    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDate
)
