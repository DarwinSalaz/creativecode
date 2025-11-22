package com.portafolio.repositories

import com.portafolio.entities.Revenue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RevenueRepository: JpaRepository<Revenue, Long> {

    @Query("SELECT e FROM Revenue e WHERE e.applicationUserId = ?1")
    fun getRevenues(applicationUserId : Long) : List<Revenue>?

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.walletId = :walletId 
          AND r.revenueDate BETWEEN :startsAt AND :endsAt
        ORDER BY r.revenueDate ASC
    """)
    fun findByWalletAndDateRange(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime): List<Revenue>

    @Query("SELECT r FROM Revenue r WHERE r.walletId = :walletId ORDER BY r.revenueDate DESC")
    fun findByWalletId(@Param("walletId") walletId: Int): List<Revenue>

    @Query("SELECT r FROM Revenue r WHERE r.revenueType = :revenueType ORDER BY r.revenueDate DESC")
    fun findByRevenueType(@Param("revenueType") revenueType: String): List<Revenue>

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.walletId = :walletId AND r.revenueType = :revenueType 
        ORDER BY r.revenueDate DESC
    """)
    fun findByWalletIdAndRevenueType(
        @Param("walletId") walletId: Int,
        @Param("revenueType") revenueType: String
    ): List<Revenue>

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.walletId = :walletId 
          AND r.revenueDate BETWEEN :startDate AND :endDate
        ORDER BY r.revenueDate DESC
    """)
    fun findByWalletIdAndDateRange(
        @Param("walletId") walletId: Int,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Revenue>

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.revenueType = :revenueType 
          AND r.revenueDate BETWEEN :startDate AND :endDate
        ORDER BY r.revenueDate DESC
    """)
    fun findByRevenueTypeAndDateRange(
        @Param("revenueType") revenueType: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Revenue>

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.walletId = :walletId 
          AND r.revenueType = :revenueType 
          AND r.revenueDate BETWEEN :startDate AND :endDate
        ORDER BY r.revenueDate DESC
    """)
    fun findByWalletIdAndRevenueTypeAndDateRange(
        @Param("walletId") walletId: Int,
        @Param("revenueType") revenueType: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Revenue>

    @Query("""
        SELECT r FROM Revenue r 
        WHERE r.revenueDate BETWEEN :startDate AND :endDate
        ORDER BY r.revenueDate DESC
    """)
    fun findByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Revenue>

}