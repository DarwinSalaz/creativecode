package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.portafolio.entities.Product

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductResponse (

    val status: String = "OK",

    val products: List<Product>

)