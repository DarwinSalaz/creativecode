package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.portafolio.entities.MovementType
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class InventoryMovementRequest(
    @JsonProperty("product_id")
    val productId: Long,
    @JsonProperty("movement_type")
    val movementType: MovementType,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("description")
    val description: String? = null,
    @JsonProperty("wallet_id")
    val walletId: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InventoryMovementResponse(
    @JsonProperty("id")
    val id: Long,
    @JsonProperty("product_id")
    val productId: Long,
    @JsonProperty("product_name")
    val productName: String,
    @JsonProperty("movement_type")
    val movementType: MovementType,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("previous_quantity")
    val previousQuantity: Int,
    @JsonProperty("new_quantity")
    val newQuantity: Int,
    @JsonProperty("user_id")
    val userId: Long,
    @JsonProperty("username")
    val username: String,
    @JsonProperty("movement_date")
    val movementDate: LocalDateTime,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("wallet_id")
    val walletId: Int,
    @JsonProperty("wallet_name")
    val walletName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InventoryMovementListResponse(
    @JsonProperty("movements")
    val movements: List<InventoryMovementResponse>,
    @JsonProperty("total_movements")
    val totalMovements: Int
) 