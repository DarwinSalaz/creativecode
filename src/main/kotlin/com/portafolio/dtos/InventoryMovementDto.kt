package com.portafolio.dtos

import com.portafolio.entities.MovementType
import java.time.LocalDateTime

data class InventoryMovementRequest(
    val productId: Long,
    val movementType: MovementType,
    val quantity: Int,
    val description: String? = null,
    val walletId: Int
)

data class InventoryMovementResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val movementType: MovementType,
    val quantity: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val userId: Long,
    val username: String,
    val movementDate: LocalDateTime,
    val description: String?,
    val walletId: Int,
    val walletName: String
)

data class InventoryMovementListResponse(
    val movements: List<InventoryMovementResponse>,
    val totalMovements: Int
) 