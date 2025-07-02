package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class RevenueListResponseDto(

    @JsonProperty("revenue_id")
    val revenueId: Long,

    @JsonProperty("revenue_type")
    val revenueType: String,

    @JsonProperty("value")
    val value: BigDecimal,

    @JsonProperty("revenue_date")
    val revenueDate: LocalDateTime,

    @JsonProperty("justification")
    val justification: String?,

    @JsonProperty("wallet_id")
    val walletId: Int?,

    @JsonProperty("username")
    val username: String

) 