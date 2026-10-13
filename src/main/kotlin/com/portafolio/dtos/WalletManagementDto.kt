package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class WalletNameRequest(
    @field:NotBlank(message = "El nombre de la cartera es obligatorio.")
    @field:Size(max = 100, message = "El nombre de la cartera no puede superar 100 caracteres.")
    val name: String
)

data class WalletManagementResponse(
    @JsonProperty("wallet_id") val walletId: Int,
    val name: String,
    val active: Boolean
)
