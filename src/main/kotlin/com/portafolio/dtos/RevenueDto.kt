package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

class RevenueDto (

    @JsonProperty("application_user_id")
    var applicationUserId: Long = 0,

    @JsonProperty("revenue_type")
    var revenueType: String,

    @JsonProperty("value")
    val value: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("revenue_date")
    val revenueDate: LocalDateTime,

    @JsonProperty("justification")
    var justification: String?,

    @JsonProperty("wallet_id")
    var walletId: Int?,

    @JsonProperty("username")
    var username: String?

)