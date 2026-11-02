package com.portafolio.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.entities.Service
import com.portafolio.repositories.ExpenseRepository
import com.portafolio.repositories.RevenueRepository
import com.portafolio.repositories.ServiceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class ServicesServiceScheduleTest {
    private val repository = mock<ServiceRepository>()
    private lateinit var service: ServicesService

    @Before
    fun setUp() {
        service = ServicesService(mock<ExpenseRepository>(), mock<RevenueRepository>())
        ReflectionTestUtils.setField(service, "repository", repository)
    }

    @Test
    fun `reschedules an active account without creating a payment`() {
        val account = Service(serviceId = 10, debt = BigDecimal("50000"), state = "paying", hasExpiredPayment = true)
        val newDate = LocalDate.now().plusDays(7)
        whenever(repository.findById(10)).thenReturn(Optional.of(account))
        whenever(repository.save(account)).thenReturn(account)

        val result = service.rescheduleNextPayment(10, newDate)

        assertEquals(newDate, result)
        assertEquals(newDate.atStartOfDay(), account.nextPaymentDate)
        assertFalse(account.hasExpiredPayment!!)
        verify(repository).save(account)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a past next payment date`() {
        service.rescheduleNextPayment(10, LocalDate.now().minusDays(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a fully paid account`() {
        val account = Service(serviceId = 10, debt = BigDecimal.ZERO, state = "fully_paid")
        whenever(repository.findById(10)).thenReturn(Optional.of(account))

        service.rescheduleNextPayment(10, LocalDate.now().plusDays(1))
    }
}
