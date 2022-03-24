package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.*

@Table(name = "cash_movements")
@Entity
data class CashMovement(

    @Id
    @JsonProperty("cash_movement_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_movement_id")
    val cashMovementId: Long = 0,

    @JsonProperty("cash_movement_type")
    @Column(name = "cash_movement_type")
    var cashMovementType: String,

    @JsonProperty("movement_type")
    @Column(name = "movement_type")
    var movementType: String,

    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    var applicationUserId: Long,

    @JsonProperty("payment_id")
    @Column(name = "payment_id")
    var paymentId: Long,

    @JsonProperty("value")
    @Column(name = "value")
    var value: BigDecimal,

    @JsonProperty("description")
    @Column(name = "description")
    var description: String?,

    @JsonProperty("cash_control_id")
    @Column(name = "cash_control_id")
    var cashControlId: Long?,

    @JsonProperty("commission")
    @Column(name = "commission")
    var commission: BigDecimal?,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

)
