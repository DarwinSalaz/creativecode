package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class CashMovementDto(

    @JsonProperty("cash_movement_type")
    var cashMovementType: String,

    @JsonProperty("movement_type")
    var movementType: String,

    @JsonProperty("payment_id")
    var paymentId: Long?,

    @JsonProperty("value")
    var value: String,

    @JsonProperty("service_id")
    var serviceId: Long?,

    @JsonProperty("commission")
    var commission: String?,

    @JsonProperty("down_payments")
    var downPayments: String?,

    @JsonProperty("created_at")
    var createdAt: String,

    @JsonProperty("description")
    var description: String?,

    @JsonProperty("expense_id")
    var expenseId: Long?
)
