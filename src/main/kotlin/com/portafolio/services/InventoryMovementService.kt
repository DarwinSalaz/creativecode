package com.portafolio.services

import com.portafolio.dtos.InventoryMovementRequest
import com.portafolio.dtos.InventoryMovementBatchRequest
import com.portafolio.dtos.InventoryMovementResponse
import com.portafolio.entities.InventoryMovement
import com.portafolio.entities.MovementType
import com.portafolio.repositories.InventoryMovementRepository
import com.portafolio.repositories.ProductRepository
import com.portafolio.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InventoryMovementService(
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val productRepository: ProductRepository,
    private val walletRepository: WalletRepository
) {
    
    @Transactional
    fun registerMovement(request: InventoryMovementRequest, userId: Long, username: String): InventoryMovementResponse {
        // Obtener el producto
        val product = productRepository.findById(request.productId)
            .orElseThrow { IllegalArgumentException("Producto no encontrado") }
        
        // Obtener la cartera
        val wallet = walletRepository.findById(request.walletId)
            .orElseThrow { IllegalArgumentException("Cartera no encontrada") }
        
        val previousQuantity = product.leftQuantity
        val newQuantity = when (request.movementType) {
            MovementType.ENTRADA -> previousQuantity + request.quantity
            MovementType.SALIDA -> {
                if (previousQuantity < request.quantity) {
                    // TODO solventar esto
                    //throw IllegalArgumentException("Stock insuficiente. Stock actual: $previousQuantity, Cantidad solicitada: ${request.quantity}")
                }
                previousQuantity - request.quantity
            }
        }
        
        // Crear el movimiento
        val movement = InventoryMovement(
            productId = request.productId,
            movementType = request.movementType,
            quantity = request.quantity,
            previousQuantity = previousQuantity,
            newQuantity = newQuantity,
            userId = userId,
            username = username,
            description = request.description,
            walletId = request.walletId
        )
        
        // Guardar el movimiento
        val savedMovement = inventoryMovementRepository.save(movement)
        
        // Actualizar la cantidad del producto
        product.leftQuantity = newQuantity
        productRepository.save(product)
        
        // Retornar respuesta
        return InventoryMovementResponse(
            id = savedMovement.id,
            productId = savedMovement.productId,
            productName = product.name,
            movementType = savedMovement.movementType,
            quantity = savedMovement.quantity,
            previousQuantity = savedMovement.previousQuantity,
            newQuantity = savedMovement.newQuantity,
            userId = savedMovement.userId,
            username = savedMovement.username,
            movementDate = savedMovement.movementDate,
            description = savedMovement.description,
            walletId = savedMovement.walletId,
            walletName = wallet.name
        )
    }

    @Transactional
    fun registerMovements(request: InventoryMovementBatchRequest, userId: Long, username: String): List<InventoryMovementResponse> {
        val wallet = walletRepository.findById(request.walletId)
            .orElseThrow { IllegalArgumentException("Cartera no encontrada") }

        val duplicatedProduct = request.items.groupBy { it.productId }.entries.firstOrNull { it.value.size > 1 }
        if (duplicatedProduct != null) {
            throw IllegalArgumentException("No se puede cargar el mismo producto más de una vez")
        }

        request.items.forEach { item ->
            val product = productRepository.findById(item.productId)
                .orElseThrow { IllegalArgumentException("Producto no encontrado: ${item.productId}") }
            if (product.wallet.walletId != wallet.walletId) {
                throw IllegalArgumentException("El producto ${product.name} no pertenece a la cartera seleccionada")
            }
            if (request.movementType == MovementType.SALIDA && product.leftQuantity < item.quantity) {
                throw IllegalArgumentException("Stock insuficiente para ${product.name}")
            }
        }

        return request.items.map { item ->
            registerMovement(
                InventoryMovementRequest(
                    productId = item.productId,
                    movementType = request.movementType,
                    quantity = item.quantity,
                    description = request.description,
                    walletId = request.walletId
                ),
                userId,
                username
            )
        }
    }
    
    fun getMovementsByWalletIds(walletIds: List<Int>): List<InventoryMovementResponse> {
        val movements = inventoryMovementRepository.findByWalletIds(walletIds)
        return movements.map { movement ->
            val product = productRepository.findById(movement.productId).orElse(null)
            val wallet = walletRepository.findById(movement.walletId).orElse(null)
            
            InventoryMovementResponse(
                id = movement.id,
                productId = movement.productId,
                productName = product?.name ?: "Producto no encontrado",
                movementType = movement.movementType,
                quantity = movement.quantity,
                previousQuantity = movement.previousQuantity,
                newQuantity = movement.newQuantity,
                userId = movement.userId,
                username = movement.username,
                movementDate = movement.movementDate,
                description = movement.description,
                walletId = movement.walletId,
                walletName = wallet?.name ?: "Cartera no encontrada"
            )
        }
    }
    
    fun getMovementsByProductId(productId: Long): List<InventoryMovementResponse> {
        val movements = inventoryMovementRepository.findByProductId(productId)
        return movements.map { movement ->
            val product = productRepository.findById(movement.productId).orElse(null)
            val wallet = walletRepository.findById(movement.walletId).orElse(null)
            
            InventoryMovementResponse(
                id = movement.id,
                productId = movement.productId,
                productName = product?.name ?: "Producto no encontrado",
                movementType = movement.movementType,
                quantity = movement.quantity,
                previousQuantity = movement.previousQuantity,
                newQuantity = movement.newQuantity,
                userId = movement.userId,
                username = movement.username,
                movementDate = movement.movementDate,
                description = movement.description,
                walletId = movement.walletId,
                walletName = wallet?.name ?: "Cartera no encontrada"
            )
        }
    }
    
    fun getMovementsByDateRange(walletIds: List<Int>, startDate: LocalDateTime, endDate: LocalDateTime): List<InventoryMovementResponse> {
        val movements = inventoryMovementRepository.findByWalletIdsAndDateRange(walletIds, startDate, endDate)
        return movements.map { movement ->
            val product = productRepository.findById(movement.productId).orElse(null)
            val wallet = walletRepository.findById(movement.walletId).orElse(null)
            
            InventoryMovementResponse(
                id = movement.id,
                productId = movement.productId,
                productName = product?.name ?: "Producto no encontrado",
                movementType = movement.movementType,
                quantity = movement.quantity,
                previousQuantity = movement.previousQuantity,
                newQuantity = movement.newQuantity,
                userId = movement.userId,
                username = movement.username,
                movementDate = movement.movementDate,
                description = movement.description,
                walletId = movement.walletId,
                walletName = wallet?.name ?: "Cartera no encontrada"
            )
        }
    }
}
