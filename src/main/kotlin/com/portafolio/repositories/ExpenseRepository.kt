package com.portafolio.repositories

import com.portafolio.entities.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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

}