package com.portafolio.repositories

import com.portafolio.entities.CashMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CashMovementRepository: JpaRepository<CashMovement, Long> {

    @Query("SELECT c FROM CashMovement c WHERE c.cashControlId = ?1")
    fun findCashMovementsByCashControlId(cashControlId: Long): List<CashMovement>

}