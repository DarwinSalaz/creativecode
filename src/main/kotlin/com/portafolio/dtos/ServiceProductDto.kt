package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class ServiceProductDto (

    @JsonProperty("product_id")
    val productId: Long,

    val value: BigDecimal,

    val quantity: Int

)