package com.portafolio.dtos

import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import javax.validation.Validation

class CustomerDtoValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `accepts an optional complete customer location`() {
        val customer = validCustomer(
            latitude = BigDecimal("4.6512346"),
            longitude = BigDecimal("-74.0723457")
        )

        assertTrue(validator.validate(customer).isEmpty())
    }

    @Test
    fun `rejects an incomplete customer location`() {
        val customer = validCustomer(latitude = BigDecimal("4.6512346"), longitude = null)

        assertTrue(validator.validate(customer).any {
            it.message == "latitude and longitude must be provided together"
        })
    }

    @Test
    fun `rejects coordinates outside geographic ranges`() {
        val customer = validCustomer(
            latitude = BigDecimal("91"),
            longitude = BigDecimal("-181")
        )

        assertTrue(validator.validate(customer).size == 2)
    }

    private fun validCustomer(latitude: BigDecimal?, longitude: BigDecimal?) = CustomerDto(
        companyId = 1,
        name = "Cliente",
        lastName = "Prueba",
        cellphone = null,
        email = null,
        address = null,
        identificationNumber = "123",
        walletId = 1,
        latitude = latitude,
        longitude = longitude
    )
}
