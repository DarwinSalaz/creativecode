package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class RevenueDeleteResponseDto(

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("message")
    val message: String

) 