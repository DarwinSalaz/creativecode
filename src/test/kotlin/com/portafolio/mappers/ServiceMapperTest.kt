package com.portafolio.mappers

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
