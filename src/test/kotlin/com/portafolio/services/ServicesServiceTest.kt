package com.portafolio.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.entities.Service
import com.portafolio.repositories.ExpenseRepository
import com.portafolio.repositories.RevenueRepository
import com.portafolio.repositories.ServiceRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

class ServicesServiceTest {
    private val expenseRepository = mock<ExpenseRepository>()
    private val revenueRepository = mock<RevenueRepository>()
    private val serviceRepository = mock<ServiceRepository>()
    private lateinit var service: ServicesService

    @Before
    fun setUp() {
        service = ServicesService(expenseRepository, revenueRepository)
        ReflectionTestUtils.setField(service, "repository", serviceRepository)
    }

    @Test
    fun `reverting a cash sale payment does not divide by zero`() {
        val cashSale = Service(
            serviceId = 4725,
            totalValue = BigDecimal("90000"),
            debt = BigDecimal.ZERO,
            feeValue = BigDecimal.ZERO,
            pendingFees = 0,
            state = "fully_paid"
        )
        whenever(serviceRepository.findById(4725)).thenReturn(Optional.of(cashSale))
        whenever(serviceRepository.save(cashSale)).thenReturn(cashSale)

        val updated = service.updateServiceForPayment(
            serviceId = 4725,
            value = BigDecimal("-90000"),
            nextPaymentDate = null,
            depositPayment = BigDecimal.ZERO
        )

        assertEquals(BigDecimal("90000"), updated.debt)
        assertEquals(BigDecimal("90000"), updated.pendingValue)
        assertEquals(0, updated.pendingFees)
        assertEquals("paying", updated.state)
    }
}
