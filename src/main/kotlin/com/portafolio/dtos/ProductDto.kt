package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import javax.validation.constraints.NotNull

data class ProductDto(

    @field:NotNull(message = "CompanyId is notNull")
    @JsonProperty("company_id")
    val companyId: Int? = null,

    @JsonProperty("product_id")
    val productId: Long = 0,

    @field:NotNull(message = "Name is notNull")
    @JsonProperty("name")
    val name: String,

    @JsonProperty("description")
    val description: String?,

    @field:NotNull(message = "Value is notNull")
    @JsonProperty("value")
    val value: BigDecimal,

    @JsonProperty("cost")
    val cost: BigDecimal?,

    @field:NotNull(message = "LeftQuantity is notNull")
    @JsonProperty("left_quantity")
    val leftQuantity: Int,

    @JsonProperty("value_str")
    val valueStr: String?,

    @field:NotNull(message = "WalletId is notNull")
    @JsonProperty("wallet_id")
    val walletId: Int,

    @JsonProperty("value_4_installments")
    val value4Installments: BigDecimal? = null,

    @JsonProperty("value_6_installments")
    val value6Installments: BigDecimal? = null,

    @JsonProperty("value_8_installments")
    val value8Installments: BigDecimal? = null,

    @JsonProperty("value_10_installments")
    val value10Installments: BigDecimal? = null

)
