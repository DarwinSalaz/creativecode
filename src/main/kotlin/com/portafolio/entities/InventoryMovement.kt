package com.portafolio.entities

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "inventory_movements")
data class InventoryMovement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    
    @Column(name = "movement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val movementType: MovementType,
    
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    
    @Column(name = "previous_quantity", nullable = false)
    val previousQuantity: Int,
    
    @Column(name = "new_quantity", nullable = false)
    val newQuantity: Int,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Column(name = "username", nullable = false)
    val username: String,
    
    @Column(name = "movement_date", nullable = false)
    val movementDate: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "description", length = 500)
    val description: String? = null,
    
    @Column(name = "wallet_id", nullable = false)
    val walletId: Int
)

enum class MovementType {
    ENTRADA, SALIDA
} 