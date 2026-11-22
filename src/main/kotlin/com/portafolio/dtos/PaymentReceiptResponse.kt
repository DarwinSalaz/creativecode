package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentReceiptResponse(
    @JsonProperty("payment_id")
    val paymentId: Long,

    @JsonProperty("service_id")
    val serviceId: Long,

    @JsonProperty("customer_name")
    val customerName: String,

    @JsonProperty("payment_value")
    val paymentValue: BigDecimal,

    @JsonProperty("balance")
    val balance: BigDecimal,

    @JsonProperty("payment_date")
    val paymentDate: LocalDateTime,

    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDateTime?,

    @JsonProperty("company_name")
    val companyName: String,

    @JsonProperty("company_nit")
    val companyNit: String
)
