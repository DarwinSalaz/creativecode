package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.Table
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Column

@Table(name = "payments")
@Entity
data class Payment (

    @Id
    @JsonProperty("payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val paymentId: Long = 0,

    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("service_id")
    @Column(name = "service_id")
    val serviceId: Long = 0,

    @JsonProperty("value")
    @Column(name = "value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

)