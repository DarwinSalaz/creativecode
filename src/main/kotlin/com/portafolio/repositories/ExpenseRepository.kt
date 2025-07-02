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

    @Query("SELECT e FROM Expense e WHERE e.walletId = :walletId ORDER BY e.expenseDate DESC")
    fun findByWalletId(@Param("walletId") walletId: Int): List<Expense>

    @Query("SELECT e FROM Expense e WHERE e.expenseType = :expenseType ORDER BY e.expenseDate DESC")
    fun findByExpenseType(@Param("expenseType") expenseType: String): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.walletId = :walletId AND e.expenseType = :expenseType 
        ORDER BY e.expenseDate DESC
    """)
    fun findByWalletIdAndExpenseType(
        @Param("walletId") walletId: Int,
        @Param("expenseType") expenseType: String
    ): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.walletId = :walletId 
          AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
    """)
    fun findByWalletIdAndDateRange(
        @Param("walletId") walletId: Int,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.expenseType = :expenseType 
          AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
    """)
    fun findByExpenseTypeAndDateRange(
        @Param("expenseType") expenseType: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.walletId = :walletId 
          AND e.expenseType = :expenseType 
          AND e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
    """)
    fun findByWalletIdAndExpenseTypeAndDateRange(
        @Param("walletId") walletId: Int,
        @Param("expenseType") expenseType: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Expense>

    @Query("""
        SELECT e FROM Expense e 
        WHERE e.expenseDate BETWEEN :startDate AND :endDate
        ORDER BY e.expenseDate DESC
    """)
    fun findByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Expense>

}