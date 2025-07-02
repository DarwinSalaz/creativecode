package com.portafolio.repositories

import com.portafolio.entities.CashMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface CashMovementRepository: JpaRepository<CashMovement, Long> {

    @Query("SELECT c FROM CashMovement c WHERE c.cashControlId = ?1 ORDER BY c.createdAt DESC")
    fun findCashMovementsByCashControlId(cashControlId: Long): List<CashMovement>

    @Query("SELECT c FROM CashMovement c WHERE c.cashControlId = ?1 and c.cashMovementType = 'expense' ORDER BY c.createdAt DESC")
    fun getExpensesByCashControlId(cashControlId: Long): List<CashMovement>

    @Query("SELECT c FROM CashMovement c WHERE c.walletId = ?1 AND c.createdAt BETWEEN ?2 AND ?3")
    fun getCashMovementsByWallet(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime): List<CashMovement>

    @Transactional
    fun deleteByExpenseId(expenseId: Long)

    @Transactional
    fun deleteByRevenueId(revenueId: Long)

}