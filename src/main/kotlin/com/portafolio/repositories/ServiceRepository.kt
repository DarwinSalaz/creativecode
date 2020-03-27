package com.portafolio.repositories

import com.portafolio.entities.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface ServiceRepository: JpaRepository<Service, Long> {

    @Query("SELECT s FROM Service s WHERE s.customerId = ?1")
    fun findAllServicesByUser(customerId : Long) : List<Service>?

    @Modifying
    @Query("UPDATE Service s SET s.debt = s.debt - ?1, s.nextPaymentDate = ?2 where s.serviceId = ?3")
    fun updateDebtService(value : BigDecimal, nextPaymentDate: LocalDateTime?, serviceId: Long)

}