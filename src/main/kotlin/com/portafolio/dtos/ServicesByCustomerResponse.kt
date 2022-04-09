package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

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
    var serviceProducts: MutableList<ServiceProductDto> = mutableListOf()

)