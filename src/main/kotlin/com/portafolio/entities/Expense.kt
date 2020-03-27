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

@Table(name = "expenses")
@Entity
data class Expense (

    @Id
    @JsonProperty("expense_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    val expenseId: Long = 0,

    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("expense_type")
    @Column(name = "expense_type")
    var expenseType: String,

    @JsonProperty("value")
    @Column(name = "value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("expense_date")
    @Column(name = "expense_date")
    val expenseDate: LocalDateTime,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),

    @JsonProperty("justification")
    @Column(name = "justification")
    var justification: String?

)