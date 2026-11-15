package com.portafolio.mappers

import com.portafolio.dtos.ServiceDto
import com.portafolio.models.PaymentReportInterface
import com.portafolio.services.Utilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class ServiceMapperTest {
    private lateinit var mapper: ServiceMapper

    @Before
    fun setUp() {
        mapper = ServiceMapper()
        mapper.utilities = Utilities()
    }

    @Test
    fun `payment report totals debt once per service`() {
        val report = mapper.mapPaymentReport(listOf(
            payment(1, 101, "10000", "80000"),
            payment(2, 101, "10000", "80000"),
            payment(3, 202, "5000", "45000")
        ))

        assertEquals("\$25.000", report.totalValue)
        assertEquals("\$125.000", report.totalDebt)
        assertEquals(3, report.paymentsData.size)
    }

    @Test
    fun `direct purchase is normalized as fully paid without down payment`() {
        val service = mapper.map(ServiceDto(
            serviceId = 0,
            applicationUserId = 0,
            serviceValue = BigDecimal("100000"),
            downPayment = BigDecimal("10000"),
            discount = BigDecimal.ZERO,
            debt = BigDecimal("100000"),
            totalValue = BigDecimal("100000"),
            daysPerFee = 7,
            quantityOfFees = 10,
            feeValue = BigDecimal("11111"),
            walletId = 1,
            customerId = 2,
            observations = null,
            pendingFees = 9,
            payDownInInstallments = true,
            directPurchase = true
        ), applicationUserId = 8)

        assertEquals(BigDecimal.ZERO, service.downPayment)
        assertEquals(BigDecimal.ZERO, service.downPaymentTotal)
        assertEquals(BigDecimal.ZERO, service.debt)
        assertEquals(BigDecimal.ZERO, service.feeValue)
        assertEquals(0, service.pendingFees)
        assertEquals(false, service.payDownInInstallments)
    }

    private fun payment(id: Long, serviceId: Long, value: String, debt: String) = object : PaymentReportInterface {
        override var id: Long? = id
        override var client: String? = "Cliente"
        override var service_id: Long? = serviceId
        override var value: BigDecimal? = BigDecimal(value)
        override var debt: BigDecimal? = BigDecimal(debt)
        override var products: String? = "Producto"
        override var service_created_at: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 10, 0)
        override var wallet: String? = "Cartera"
        override var username: String? = "admin"
        override var created_at: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0)
    }
}
