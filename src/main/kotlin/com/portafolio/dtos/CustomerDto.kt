package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.AssertTrue
import java.math.BigDecimal

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
    val walletId: Int,

    @field:DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
    @field:DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
    @JsonProperty("latitude")
    val latitude: BigDecimal? = null,

    @field:DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
    @field:DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
    @JsonProperty("longitude")
    val longitude: BigDecimal? = null,

    @JsonProperty("clear_location")
    val clearLocation: Boolean = false

) {
    @get:AssertTrue(message = "latitude and longitude must be provided together")
    @get:JsonIgnore
    val isLocationComplete: Boolean
        get() = clearLocation || (latitude == null && longitude == null) || (latitude != null && longitude != null)
}
