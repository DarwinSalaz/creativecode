package com.portafolio.dtos

import com.portafolio.entities.Customer

data class CustomersResponse (

    val status: String = "OK",

    val customers: List<Customer>

)