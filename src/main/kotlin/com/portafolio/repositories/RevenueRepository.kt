package com.portafolio.repositories

import com.portafolio.entities.Revenue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RevenueRepository: JpaRepository<Revenue, Long> {

    @Query("SELECT e FROM Revenue e WHERE e.applicationUserId = ?1")
    fun getRevenues(applicationUserId : Long) : List<Revenue>?

}