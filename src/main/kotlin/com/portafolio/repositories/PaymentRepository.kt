package com.portafolio.repositories

import com.portafolio.entities.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository: JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.serviceId = ?1")
    fun findAllPaymentsByServiceId(serviceId : Long) : List<Payment>?

}