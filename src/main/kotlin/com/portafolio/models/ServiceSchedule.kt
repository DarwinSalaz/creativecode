package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class ServiceSchedule(

    @JsonProperty("customer_id")
    val customerId: Long?,

    @JsonProperty("name")
    var name: String?,

    @JsonProperty("last_name")
    var lastName: String?,

    @JsonProperty("icon")
    var icon: String?,

    @JsonProperty("fee_value")
    val feeValue: BigDecimal?,

    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDateTime?

)
