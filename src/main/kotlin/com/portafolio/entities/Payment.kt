package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
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
    @Column(name = "application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("service_id")
    @Column(name = "service_id")
    val serviceId: Long = 0,

    @JsonProperty("value")
    @Column(name = "value")
    val value: BigDecimal = BigDecimal.ZERO

)