package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.portafolio.entities.MovementType
import java.time.LocalDateTime
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

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
data class InventoryMovementBatchItem(
    @JsonProperty("product_id")
    val productId: Long,
    @field:Min(1, message = "La cantidad debe ser mayor que cero")
    @JsonProperty("quantity")
    val quantity: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InventoryMovementBatchRequest(
    @JsonProperty("wallet_id")
    val walletId: Int,
    @field:NotNull(message = "El tipo de movimiento es obligatorio")
    @JsonProperty("movement_type")
    val movementType: MovementType,
    @JsonProperty("description")
    val description: String? = null,
    @field:Valid
    @field:NotEmpty(message = "Debe incluir al menos un producto")
    @JsonProperty("items")
    val items: List<InventoryMovementBatchItem>
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
