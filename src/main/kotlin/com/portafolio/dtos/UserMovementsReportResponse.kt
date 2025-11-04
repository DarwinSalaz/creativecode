package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class UserMovementsReportResponse(
    
    // Encabezado
    @JsonProperty("user_full_name")
    val userFullName: String,
    
    @JsonProperty("username")
    val username: String,
    
    @JsonProperty("starts_at")
    val startsAt: LocalDateTime,
    
    @JsonProperty("ends_at")
    val endsAt: LocalDateTime,
    
    @JsonProperty("period_label")
    val periodLabel: String,
    
    // Totales
    @JsonProperty("total_inputs")
    val totalInputs: String,
    
    @JsonProperty("total_inputs_number")
    val totalInputsNumber: BigDecimal,
    
    @JsonProperty("total_outputs")
    val totalOutputs: String,
    
    @JsonProperty("total_outputs_number")
    val totalOutputsNumber: BigDecimal,
    
    @JsonProperty("total_commissions")
    val totalCommissions: String,
    
    @JsonProperty("total_commissions_number")
    val totalCommissionsNumber: BigDecimal,
    
    @JsonProperty("total_down_payments")
    val totalDownPayments: String,
    
    @JsonProperty("total_down_payments_number")
    val totalDownPaymentsNumber: BigDecimal,
    
    @JsonProperty("net_balance")
    val netBalance: String,
    
    @JsonProperty("net_balance_number")
    val netBalanceNumber: BigDecimal,
    
    @JsonProperty("movements_count")
    val movementsCount: Int,
    
    // Detalle
    @JsonProperty("movements")
    val movements: List<UserMovementDetailDto>
)

