package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import javax.validation.constraints.NotNull

data class CancelServiceRequest(

    @field:NotNull
    @JsonProperty("service_id")
    val serviceId: Long,

    @field:NotNull
    @JsonProperty("product_ids")
    val productIds: List<Long>,

    @field:NotNull
    @JsonProperty("discount")
    val discount: BigDecimal

)
