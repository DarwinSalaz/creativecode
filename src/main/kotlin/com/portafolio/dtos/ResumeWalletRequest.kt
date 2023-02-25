package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

data class ResumeWalletRequest (

    @field:NotNull
    @JsonProperty("wallet_id")
    val walletId: Int,

    @field:NotNull
    @JsonProperty("starts_at")
    val startsAt: LocalDateTime,

    @field:NotNull
    @JsonProperty("ends_at")
    val endsAt: LocalDateTime

)
