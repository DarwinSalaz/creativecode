package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonProperty

data class InventoryDetail(

    @JsonProperty("product_id")
    var productId: Long?,

    @JsonProperty("product_name")
    var productName: String?,

    @JsonProperty("quantity_sold")
    var quantitySold: Int?,

    @JsonProperty("left_quantity")
    var leftQuantity: Int?,

    @JsonProperty("wallet_name")
    var walletName: String?

)
