package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ServiceScheduleResponse(

    @JsonProperty("customer_id")
    val customerId: Long?,

    @JsonProperty("name")
    var name: String?,

    @JsonProperty("last_name")
    var lastName: String?,

    @JsonProperty("icon")
    var icon: String?,

    @JsonProperty("fee_value")
    val feeValue: String?,

    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDate?

)
