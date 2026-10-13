package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class LiquidationReportColumnDto(
    @JsonProperty("liquidation_id") val liquidationId: Long,
    @JsonProperty("cash_control_id") val cashControlId: Long,
    @JsonProperty("liquidated_at") val liquidatedAt: LocalDateTime,
    val value: BigDecimal
)

data class LiquidationReportRowDto(
    @JsonProperty("service_id") val serviceId: Long,
    @JsonProperty("wallet_id") val walletId: Int,
    @JsonProperty("created_at") val createdAt: LocalDateTime,
    val name: String,
    @JsonProperty("last_name") val lastName: String?,
    val cellphone: String?,
    val address: String?,
    @JsonProperty("identification_number") val identificationNumber: String?,
    val observation: String?,
    val articles: List<String>,
    @JsonProperty("fee_value") val feeValue: BigDecimal,
    @JsonProperty("quantity_of_fees") val quantityOfFees: Int,
    @JsonProperty("total_payments") val totalPayments: BigDecimal,
    @JsonProperty("down_payment") val downPayment: BigDecimal,
    val debt: BigDecimal,
    @JsonProperty("liquidation_values") val liquidationValues: Map<Long, BigDecimal>
)

data class LiquidationPaymentsReportDto(
    @JsonProperty("scope_label") val scopeLabel: String,
    @JsonProperty("wallet_id") val walletId: Int?,
    @JsonProperty("cash_control_id") val cashControlId: Long?,
    val liquidations: List<LiquidationReportColumnDto>,
    val rows: List<LiquidationReportRowDto>
)
