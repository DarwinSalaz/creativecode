package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonProperty

data class ServiceReportResponse(

    @JsonProperty("total_product_values")
    var totalProductValues: String?,

    @JsonProperty("total_discount")
    var totalDiscount: String?,

    @JsonProperty("total_service_value")
    var totalServiceValue: String?,

    @JsonProperty("total_debt")
    var totalDebt: String?,

    @JsonProperty("services_data")
    var servicesData: List<ServiceReport>,

    @JsonProperty("products_sold")
    var productsSold: List<ProductSoldReport> = emptyList()

)

data class ProductSoldReport(
    @JsonProperty("product_id")
    val productId: Int?,

    @JsonProperty("product_name")
    val productName: String,

    @JsonProperty("total_quantity")
    val totalQuantity: Int,

    @JsonProperty("value")
    val value: String?,

    @JsonProperty("wallet")
    val wallet: String?
)

interface ProductSoldReportInt {
    val productId: Int?
    val productName: String
    val totalQuantity: Int
    val totalValue: java.math.BigDecimal?
    val walletName: String?
}