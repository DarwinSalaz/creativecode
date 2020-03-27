package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationUserCreateDto (

    @JsonProperty("company_id")
    val companyId: Int,

    val username: String,

    val name: String,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("cellphone")
    val cellphone: String? = null,

    @JsonProperty("email")
    val email: String? = null,

    var password: String,

    @JsonProperty("user_profile_id")
    val userProfileId: Int,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("wallet_ids")
    val walletIds: List<Int>?

)