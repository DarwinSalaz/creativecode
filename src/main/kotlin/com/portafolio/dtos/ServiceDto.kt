package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Column
import javax.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceUpdateDto(
    @field:NotNull(message = "service_id is notNull")
    @JsonProperty("service_id")
    var serviceId: Long,

    @field:NotNull(message = "quantity_of_fees is notNull")
    @JsonProperty("quantity_of_fees")
    val quantityOfFees: Int,

    @field:NotNull(message = "fee_value is notNull")
    @JsonProperty("fee_value")
    val feeValue: BigDecimal = BigDecimal.valueOf(0)
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceDto (

    @JsonProperty("service_id")
    var serviceId: Long,

    @JsonProperty("application_user_id")
    var applicationUserId: Long,

    @field:NotNull(message = "service_value is notNull")
    @JsonProperty("service_value")
    val serviceValue: BigDecimal = BigDecimal.valueOf(0),

    @field:NotNull(message = "down_payment is notNull")
    @JsonProperty("down_payment")
    val downPayment: BigDecimal = BigDecimal.valueOf(0),

    @field:NotNull(message = "discount is notNull")
    @JsonProperty("discount")
    val discount: BigDecimal = BigDecimal.valueOf(0),

    @field:NotNull(message = "debt is notNull")
    val debt: BigDecimal = BigDecimal.valueOf(0),

    @field:NotNull(message = "totalValue is notNull")
    @JsonProperty("total_value")
    val totalValue: BigDecimal = BigDecimal.valueOf(0),

    @field:NotNull(message = "days_per_fee is notNull")
    @JsonProperty("days_per_fee")
    val daysPerFee: Int,

    @field:NotNull(message = "quantity_of_fees is notNull")
    @JsonProperty("quantity_of_fees")
    val quantityOfFees: Int,

    @field:NotNull(message = "fee_value is notNull")
    @JsonProperty("fee_value")
    val feeValue: BigDecimal = BigDecimal.valueOf(0),

    @JsonProperty("initial_payment")
    val initialPayment: BigDecimal? = BigDecimal.valueOf(0),

    @field:NotNull(message = "walletId is notNull")
    @JsonProperty("wallet_id")
    val walletId: Int,

    @field:NotNull(message = "customerId is notNull")
    @JsonProperty("customer_id")
    val customerId: Long,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val state: String = "CREATED",

    @JsonProperty(value = "service_products")
    var serviceProducts: MutableList<ServiceProductDto> = mutableListOf(),

    @JsonProperty("observations")
    val observations: String?,

    @JsonProperty("next_payment_date")
    val nextPaymentDate: LocalDateTime? = null,

    @field:NotNull(message = "pending_fees is notNull")
    @JsonProperty("pending_fees")
    val pendingFees: Int?,

    @JsonProperty("pay_down_in_installments")
    val payDownInInstallments: Boolean = false

)