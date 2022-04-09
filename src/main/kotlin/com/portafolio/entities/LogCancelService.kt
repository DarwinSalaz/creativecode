package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*

@Table(name = "log_cancel_service")
@Entity
data class LogCancelService(

    @Id
    @JsonProperty("log_cancel_service_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_cancel_service_id")
    val logCancelServiceId: Long = 0,

    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    var applicationUserId: Long,

    @JsonProperty("service_id")
    @Column(name = "service_id")
    var serviceId: Long,

    @JsonProperty("product_ids")
    @Column(name = "product_ids")
    var productIds: String,

    @JsonProperty("complete_cancellation")
    @Column(name = "complete_cancellation")
    var completeCancellation: Boolean,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

)
