package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class PaymentReport (

    @JsonProperty("id")
    var id: Long?,

    @JsonProperty("client")
    var client: String?,

    @JsonProperty("service_id")
    var serviceId: Long?,

    @JsonProperty("value")
    var value: String?,

    @JsonProperty("debt")
    var debt: String?,

    @JsonProperty("wallet")
    var wallet: String?,

    @JsonProperty("username")
    var username: String?,

    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("created_at")
    var createdAt: LocalDateTime?

)

data class ExpiredServiceReport (
    @JsonProperty("client")
    var client: String?,
    @JsonProperty("cellphone")
    var cellphone: String?,
    @JsonProperty("address")
    var address: String?,
    @JsonProperty("total_value")
    var totalValue: String?,
    @JsonProperty("debt")
    var debt: String?,
    @JsonProperty("pending_fees")
    var pendingFees: Int?,
    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("next_payment_date")
    var nextPaymentDate: LocalDateTime?,
    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("created_at")
    var createdAt: LocalDateTime?,
    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("last_payment_date")
    var lastPaymentDate: LocalDateTime?,
    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("expired_fees")
    var expiredFees: Int?

)