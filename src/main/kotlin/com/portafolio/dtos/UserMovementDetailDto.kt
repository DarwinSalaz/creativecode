package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class UserMovementDetailDto(
    
    @JsonProperty("cash_movement_id")
    val cashMovementId: Long,
    
    @JsonProperty("cash_movement_type")
    val cashMovementType: String,
    
    @JsonProperty("cash_movement_type_label")
    val cashMovementTypeLabel: String,
    
    @JsonProperty("movement_type")
    val movementType: String,
    
    @JsonProperty("movement_type_label")
    val movementTypeLabel: String,
    
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    
    @JsonProperty("value")
    val value: String,
    
    @JsonProperty("value_number")
    val valueNumber: BigDecimal,
    
    @JsonProperty("commission")
    val commission: String,
    
    @JsonProperty("commission_number")
    val commissionNumber: BigDecimal,
    
    @JsonProperty("down_payments")
    val downPayments: String,
    
    @JsonProperty("down_payments_number")
    val downPaymentsNumber: BigDecimal,
    
    @JsonProperty("description")
    val description: String?,
    
    @JsonProperty("justification")
    val justification: String?,
    
    @JsonProperty("wallet_name")
    val walletName: String?,
    
    @JsonProperty("service_id")
    val serviceId: Long?,
    
    @JsonProperty("payment_id")
    val paymentId: Long?,
    
    @JsonProperty("expense_id")
    val expenseId: Long?,
    
    @JsonProperty("revenue_id")
    val revenueId: Long?
)

