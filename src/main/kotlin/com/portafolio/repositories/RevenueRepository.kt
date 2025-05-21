package com.portafolio.repositories

import com.portafolio.entities.Revenue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
    """)
    fun findByWalletAndDateRange(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime): List<Revenue>

}