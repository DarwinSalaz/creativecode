package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class PaymentResumeDto(

    @JsonProperty("payment_id")
    val paymentId: Long?,

    @JsonProperty("value")
    val value: String?,

    @JsonProperty("username")
    val username: String?,

    @JsonProperty("created_at")
    val createdAt: String? = null

)
