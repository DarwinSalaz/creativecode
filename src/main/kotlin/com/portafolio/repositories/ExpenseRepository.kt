package com.portafolio.repositories

import com.portafolio.entities.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExpenseRepository: JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.applicationUserId = ?1")
    fun getExpenses(applicationUserId : Long) : List<Expense>?

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.walletId = :walletId 
          AND e.expenseDate BETWEEN :startsAt AND :endsAt
    """)
    fun findByWalletAndDateRange(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE (:walletId IS NULL OR e.walletId = :walletId)
          AND (:startDate IS NULL OR e.expenseDate >= :startDate)
          AND (:endDate IS NULL OR e.expenseDate <= :endDate)
          AND (:expenseType IS NULL OR e.expenseType = :expenseType)
        ORDER BY e.expenseDate DESC
    """)
    fun findExpensesWithFilters(
        @Param("walletId") walletId: Int?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        @Param("expenseType") expenseType: String?
    ): List<Expense>

}