package com.portafolio.repositories

import com.portafolio.entities.ServiceDownPaymentPayment
import org.springframework.data.jpa.repository.JpaRepository

interface ServiceDownPaymentPaymentRepository : JpaRepository<ServiceDownPaymentPayment, Long> {

    fun findByPaymentPaymentId(paymentId: Long): List<ServiceDownPaymentPayment>

}
