package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*

@Table(name = "revenues")
@Entity
data class Revenue(

    @Id
    @JsonProperty("revenue_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revenue_id")
    val revenueId: Long = 0,

    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("revenue_type")
    @Column(name = "revenue_type")
    var revenueType: String,

    @JsonProperty("value")
    @Column(name = "value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("revenue_date")
    @Column(name = "revenue_date")
    val revenueDate: LocalDateTime,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),

    @JsonProperty("justification")
    @Column(name = "justification")
    var justification: String?,

    @JsonProperty("wallet_id")
    @Column(name = "wallet_id")
    var walletId: Int?

)
