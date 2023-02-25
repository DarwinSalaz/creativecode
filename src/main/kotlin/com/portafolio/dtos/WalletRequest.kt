package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class WalletRequest (

    @JsonProperty("wallet_ids")
    val walletIds: List<Int>?,

    @JsonProperty("date")
    val date: LocalDateTime? = null,

    @JsonProperty("expired_services")
    val expiredServices: Boolean? = false

)