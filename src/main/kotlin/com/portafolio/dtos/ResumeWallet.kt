package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class ResumeWallet (

    @JsonProperty("wallet_name")
    val walletMame: String,

    @JsonProperty("services_count")
    val servicesCount: Int,

    @JsonProperty("cash")
    val cash: String,

    @JsonProperty("down_payments")
    val downPayments: String,

    @JsonProperty("commissions")
    val commissions: String,

    @JsonProperty("expenses")
    val expenses: String

)
