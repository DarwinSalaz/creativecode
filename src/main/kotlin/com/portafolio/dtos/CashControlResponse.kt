package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

data class CashControlResponse (

    @JsonProperty("full_name")
    val fullName: String,

    @JsonProperty("cash_control_id")
    val cashControlId: Long,

    @JsonProperty("application_user_id")
    val applicationUserId: Long,

    @JsonProperty("starts_date")
    val startsDate: LocalDateTime,

    @JsonProperty("ends_date")
    val endsDate: LocalDateTime? = null,

    @JsonProperty("cash")
    val cash: String,

    @JsonProperty("revenues")
    val revenues: String,

    @JsonProperty("expenses")
    val expenses: String,

    @JsonProperty("active")
    val active: Boolean,

    @JsonProperty("period")
    val period: String,

    @JsonProperty("services_count")
    val servicesCount: Int,

    @JsonProperty("cash_number")
    val cashNumber: BigDecimal,

    @JsonProperty("commission")
    val commission: String,

    @JsonProperty("commission_number")
    val commissionNumber: BigDecimal,

    @JsonProperty("down_payments")
    val downPayments: String,

    @JsonProperty("down_payments_number")
    val downPaymentsNumber: BigDecimal,

    @JsonProperty("movements")
    val movements: List<CashMovementDto>? = null

)