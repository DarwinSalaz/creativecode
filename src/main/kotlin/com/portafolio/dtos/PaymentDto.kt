package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentDto (

    @JsonProperty("application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("service_id")
    val serviceId: Long = 0,

    @JsonProperty("value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDateTime? = null,

    @JsonProperty("deposit_payment")
    val depositPayment: BigDecimal = BigDecimal.ZERO

)