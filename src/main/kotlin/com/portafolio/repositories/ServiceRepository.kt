package com.portafolio.repositories

import com.portafolio.entities.Service
import com.portafolio.models.ServiceSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface ServiceRepository: JpaRepository<Service, Long> {

    @Query("SELECT s FROM Service s WHERE s.customerId = ?1 and s.state in ('created', 'paying')")
    fun findAllServicesByUser(customerId : Long) : List<Service>?

    @Modifying
    @Query("UPDATE Service s SET s.debt = s.debt - ?1, s.nextPaymentDate = ?2 where s.serviceId = ?3")
    fun updateDebtService(value : BigDecimal, nextPaymentDate: LocalDateTime?, serviceId: Long)

    @Query("SELECT new com.portafolio.models.ServiceSchedule(c.customerId, c.name, c.lastName, c.icon, s.feeValue, s.nextPaymentDate) FROM Service s INNER JOIN Customer c ON (s.customerId = c.customerId) WHERE c.walletId in ?1 and s.state in ('created', 'paying')")
    fun findServicesSchedule(walletIds: List<Int>) : List<ServiceSchedule>?

    @Query("SELECT new com.portafolio.models.ServiceSchedule(c.customerId, c.name, c.lastName, c.icon, s.feeValue, s.nextPaymentDate) FROM Service s INNER JOIN Customer c ON (s.customerId = c.customerId) WHERE s.state in ('created', 'paying')")
    fun findServicesSchedule() : List<ServiceSchedule>?

    @Modifying
    @Query("UPDATE Service s SET s.nextPaymentDate = ?1 where s.serviceId = ?2")
    fun updateNextPaymentDateService(nextPaymentDate: LocalDateTime?, serviceId: Long)

}