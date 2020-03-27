package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerDto (

    @field:NotNull(message = "CompanyId is notNull")
    @JsonProperty("company_id")
    val companyId: Int,

    @field:NotNull(message = "name is notNull")
    @JsonProperty("name")
    val name: String,

    @JsonProperty("last_name")
    val lastName: String?,

    @JsonProperty("cellphone")
    val cellphone: String?,

    @JsonProperty("email")
    val email: String?,

    @JsonProperty("address")
    val address: String?,

    @JsonProperty("identification_number")
    val identificationNumber: String?,

    @JsonProperty("active")
    val active: Boolean = true,

    @JsonProperty("gender")
    val gender: String? = null,

    @JsonProperty("observation")
    val observation: String? = null,

    @JsonProperty("wallet_id")
    val walletId: Int

)