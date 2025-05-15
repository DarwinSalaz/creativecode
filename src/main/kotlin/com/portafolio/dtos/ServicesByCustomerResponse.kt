package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Column

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServicesByCustomerResponse (

    @JsonProperty("service_id")
    var serviceId: Long,

    @JsonProperty("application_user_id")
    var applicationUserId: Long,

    @JsonProperty("service_value")
    val serviceValue: String,

    @JsonProperty("down_payment")
    val downPayment: String,

    @JsonProperty("down_payment_number")
    val downPaymentNumber: BigDecimal,

    @JsonProperty("discount")
    val discount: String,

    val debt: String,

    @JsonProperty("debt_in_number")
    val debtInNumber: BigDecimal,

    @JsonProperty("total_value")
    val totalValue: String,

    @JsonProperty("days_per_fee")
    val daysPerFee: Int,

    @JsonProperty("quantity_of_fees")
    val quantityOfFees: Int,

    @JsonProperty("fee_value")
    val feeValue: String,

    @JsonProperty("total_value_number")
    val totalValueNumber: BigDecimal,

    @JsonProperty("wallet_id")
    val walletId: Int,

    @JsonProperty("customer_id")
    val customerId: Long,

    @JsonProperty("created_at")
    val createdAt: String,

    val state: String = "CREATED",

    @JsonProperty("observations")
    val observations: String? = null,

    @JsonProperty("pending_value")
    var pendingValue: String?,

    @JsonProperty("payments")
    var payments: List<PaymentResumeDto> = mutableListOf(),

    @JsonProperty("service_products")
    var serviceProducts: MutableList<ServiceProductDto> = mutableListOf(),

    @JsonProperty("pending_fees")
    val pendingFees: Int?,

    @JsonProperty( "next_payment_date")
    var nextPaymentDate: String? = null

)