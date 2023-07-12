package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ServiceReport(

    @JsonProperty("id")
    var id: Long?,

    @JsonProperty("client")
    var client: String?,

    @JsonProperty("products")
    var products: String?,

    @JsonProperty("product_values")
    var productValues: String?,

    @JsonProperty("discount")
    var discount: String?,

    @JsonProperty("service_value")
    var serviceValue: String?,

    @JsonProperty("debt")
    var debt: String?,

    @JsonProperty("wallet")
    var wallet: String?,

    @JsonProperty("username")
    var username: String?,

    @JsonFormat(pattern="yyyy-MM-dd")
    @JsonProperty("created_at")
    var createdAt: LocalDateTime?

)
