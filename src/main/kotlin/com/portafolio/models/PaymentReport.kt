package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class PaymentReport (

    @JsonProperty("id")
    var id: Long?,

    @JsonProperty("client")
    var client: String?,

    @JsonProperty("service_id")
    var serviceId: Long?,

    @JsonProperty("value")
    var value: String?,

    @JsonProperty("wallet")
    var wallet: String?,

    @JsonProperty("username")
    var username: String?,

    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("created_at")
    var createdAt: LocalDateTime?

)