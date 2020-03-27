package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.Payment
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

    @Transactional
    fun save(payment: Payment, nextPaymentDate: LocalDateTime?) : Payment {

        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(payment.applicationUserId)

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = payment.applicationUserId,
                active = true,
                cash = payment.value,
                expenses = BigDecimal.ZERO,
                revenues = payment.value,
                startsDate = LocalDateTime.now(),
                servicesCount = 1
            )

            cashControlService.save(cashControl)
        } else {
            cashControlService.updateValueForNewService(activeCashControl, payment.value)
        }

        serviceRepository.updateDebtService(payment.value, nextPaymentDate, payment.serviceId)

        return repository.save(payment)
    }

}