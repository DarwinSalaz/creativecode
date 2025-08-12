package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*

@Table(name = "payments")
@Entity
data class Payment (

    @Id
    @JsonProperty("payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    val paymentId: Long = 0,

    @JsonProperty("application_user_id")
    @ManyToOne(fetch= FetchType.LAZY, cascade= [CascadeType.PERSIST])
    @JoinColumn(name = "application_user_id")
    var applicationUser: ApplicationUser,

    @JsonProperty("service_id")
    @Column(name = "service_id")
    val serviceId: Long = 0,

    @JsonProperty("value")
    @Column(name = "value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("status")
    @Column(name = "status")
    var status: String = "created",

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),

    @JsonProperty("canceled_at")
    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null,

    @JsonProperty("canceled_by")
    @Column(name = "canceled_by")
    var canceledBy: String? = null

)