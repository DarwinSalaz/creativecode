package com.portafolio.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.Payment
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.PaymentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

class PaymentServiceTest {
    private val paymentRepository = mock<PaymentRepository>()
    private val cashMovementRepository = mock<CashMovementRepository>()
    private lateinit var service: PaymentService

    @Before
    fun setUp() {
        service = PaymentService()
        ReflectionTestUtils.setField(service, "repository", paymentRepository)
        ReflectionTestUtils.setField(service, "cashMovementRepository", cashMovementRepository)
    }

    @Test
    fun `canceling a zero payment has no monetary side effects`() {
        val administrator = ApplicationUser(
            applicationUserId = 8,
            companyId = 1,
            username = "admin",
            name = "Admin",
            password = "unused",
            userProfileId = 1,
            active = true
        )
        val payment = Payment(
            paymentId = 21,
            applicationUser = administrator,
            serviceId = 15,
            value = BigDecimal.ZERO
        )
        whenever(paymentRepository.findById(21)).thenReturn(Optional.of(payment))
        whenever(paymentRepository.save(payment)).thenReturn(payment)

        val canceled = service.cancelPayment(21, administrator)

        assertEquals("canceled", canceled.status)
        assertEquals("admin", canceled.canceledBy)
        assertNotNull(canceled.canceledAt)
        verify(paymentRepository).save(payment)
        verify(cashMovementRepository, never()).findCashMovementsByPaymentId(21)
    }
}
