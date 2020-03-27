package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class WalletRequest (

    @JsonProperty("wallet_ids")
    val walletIds: List<Int>?

)