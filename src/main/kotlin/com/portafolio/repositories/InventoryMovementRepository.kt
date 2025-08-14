package com.portafolio.repositories

import com.portafolio.entities.InventoryMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InventoryMovementRepository : JpaRepository<InventoryMovement, Long> {
    
    @Query("SELECT im FROM InventoryMovement im WHERE im.walletId IN :walletIds ORDER BY im.movementDate DESC")
    fun findByWalletIds(@Param("walletIds") walletIds: List<Int>): List<InventoryMovement>
    
    @Query("SELECT im FROM InventoryMovement im WHERE im.productId = :productId ORDER BY im.movementDate DESC")
    fun findByProductId(@Param("productId") productId: Long): List<InventoryMovement>
    
    @Query("SELECT im FROM InventoryMovement im WHERE im.walletId IN :walletIds AND im.movementDate BETWEEN :startDate AND :endDate ORDER BY im.movementDate DESC")
    fun findByWalletIdsAndDateRange(
        @Param("walletIds") walletIds: List<Int>,
        @Param("startDate") startDate: java.time.LocalDateTime,
        @Param("endDate") endDate: java.time.LocalDateTime
    ): List<InventoryMovement>
} 