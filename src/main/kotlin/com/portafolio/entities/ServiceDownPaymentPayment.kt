package com.portafolio.entities

import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "service_down_payment_payments")
data class ServiceDownPaymentPayment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "service_id")
    val service: Service,

    @ManyToOne
    @JoinColumn(name = "payment_id")
    val payment: Payment,

    @Column(name = "value")
    val value: BigDecimal,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
