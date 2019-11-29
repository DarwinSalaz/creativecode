package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.GenerationType
import javax.persistence.Column

@Table(name = "cash_control")
@Entity
data class CashControl (

    @Id
    @JsonProperty("cash_control_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_control_id")
    val cashControlId: Long = 0,

    @Column(name = "application_user_id")
    val applicationUserId: Long = 0,

    @Column(name = "starts_date")
    val startsDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ends_date")
    val endsDate: LocalDateTime? = null,

    @Column(name = "cash")
    val cash: BigDecimal,

    @Column(name = "revenues")
    val revenues: BigDecimal,

    @Column(name = "expenses")
    val expenses: BigDecimal,

    @Column(name = "active")
    val active: Boolean,

    @Column(name = "services_count")
    val servicesCount: Int

)