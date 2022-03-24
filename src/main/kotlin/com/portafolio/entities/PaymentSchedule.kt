package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.*

@Table(name = "payment_schedule")
@Entity
data class PaymentSchedule(

    @Id
    @JsonProperty("payment_schedule_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_schedule_id")
    val paymentScheduleId: Long = 0,

    @JsonProperty("service_id")
    @Column(name = "service_id")
    var serviceId: Long,

    @JsonProperty("payment_date")
    @Column(name = "payment_date")
    var paymentDate: LocalDateTime,

    @JsonProperty("value")
    @Column(name = "value")
    var value: BigDecimal,

    @JsonProperty("status")
    @Column(name = "status")
    var status: String,

    @JsonProperty("payment_num")
    @Column(name = "payment_num")
    var paymentNum: Int,

    @JsonProperty("note")
    @Column(name = "note")
    var note: String?,

    @JsonProperty("associated_payment_id")
    @Column(name = "associated_payment_id")
    var associatedPaymentId: Long?,

    @JsonProperty("customer_id")
    @Column(name = "customer_id")
    var customerId: Long

)
