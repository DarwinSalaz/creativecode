package com.portafolio.repositories

import com.portafolio.entities.ServiceDownPaymentPayment
import org.springframework.data.jpa.repository.JpaRepository

interface ServiceDownPaymentPaymentRepository : JpaRepository<ServiceDownPaymentPayment, Long> {

    fun findByPaymentPaymentId(paymentId: Long): List<ServiceDownPaymentPayment>

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ServiceDownPaymentPayment s WHERE s.payment.paymentId = :paymentId")
    fun deleteByPaymentId(paymentId: Long)

}
