package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Payment
import com.portafolio.entities.PaymentSchedule
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.PaymentRepository
import com.portafolio.repositories.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class PaymentService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: PaymentRepository

    @Autowired
    private lateinit var serviceRepository: ServiceRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Transactional
    fun save(payment: Payment, nextPaymentDate: LocalDateTime?) : Payment {

        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(payment.applicationUserId)
        val cashControlId: Long
        val commission = payment.value.multiply(0.12.toBigDecimal())

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = payment.applicationUserId,
                active = true,
                cash = payment.value,
                expenses = BigDecimal.ZERO,
                commission = commission,
                revenues = payment.value,
                startsDate = LocalDateTime.now(),
                servicesCount = 1
            )

            val cashControlSaved = cashControlService.save(cashControl)

            cashControlId = cashControlSaved.cashControlId
        } else {
            cashControlService.updateValueForInputCash(activeCashControl, payment.value)

            cashControlId = activeCashControl.cashControlId
        }

        serviceRepository.updateDebtService(payment.value, nextPaymentDate, payment.serviceId)

        val paymentSaved = repository.save(payment)

        val cashMovement = CashMovement(
            cashMovementType = "fee_payment",
            movementType = "IN",
            applicationUserId = payment.applicationUserId,
            paymentId = paymentSaved.paymentId,
            value = payment.value,
            description = null,
            cashControlId = cashControlId,
            commission = commission
        )

        cashMovementRepository.save(cashMovement)

        return paymentSaved
    }

}