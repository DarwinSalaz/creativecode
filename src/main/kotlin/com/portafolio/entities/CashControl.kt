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
    var endsDate: LocalDateTime? = null,

    @Column(name = "cash")
    val cash: BigDecimal,

    @Column(name = "revenues")
    val revenues: BigDecimal,

    @Column(name = "expenses")
    val expenses: BigDecimal,

    @Column(name = "active")
    var active: Boolean,

    @Column(name = "services_count")
    var servicesCount: Int,

    @Column(name = "commission")
    var commission: BigDecimal? = null,

    @Column(name = "closure_user")
    var closureUser: String? = null,

    @Column(name = "closure_value_received")
    var closureValueReceived: BigDecimal? = null,

    @Column(name = "closure_date")
    var closureDate: LocalDateTime? = null,

    @Column(name = "closure_notes")
    var closureNotes: String? = null,

    @Column(name = "down_payments")
    var downPayments: BigDecimal? = null

)