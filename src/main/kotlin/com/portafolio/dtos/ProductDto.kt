package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.portafolio.entities.Company
import java.math.BigDecimal

data class ProductDto(

    @JsonProperty("product_id")
    val productId: Long = 0,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("value")
    val value: BigDecimal,

    @JsonProperty("wallet_id")
    val walletId: Int?

)
