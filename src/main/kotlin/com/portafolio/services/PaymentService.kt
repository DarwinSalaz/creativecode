package com.portafolio.services

import com.portafolio.entities.*
import com.portafolio.repositories.*
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
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var servicesService: ServicesService

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    private lateinit var serviceDownPaymentPaymentRepository: ServiceDownPaymentPaymentRepository

    @Transactional
    fun savePayment(serviceId: Long, value: BigDecimal, applicationUser: ApplicationUser): Payment {
        val payment = Payment(
            applicationUser = applicationUser,
            serviceId = serviceId,
            value = value,
            createdAt = LocalDateTime.now()
        )

        return repository.save(payment)
    }

    @Transactional
    fun save(payment: Payment, nextPaymentDate: LocalDateTime?, depositPayment: BigDecimal?) : Payment {

        //validate if the user has an active cash control
        val activeCashControl : CashControl = cashControlService.findActiveCashControlByUser(payment.applicationUser.applicationUserId)
        val commission = payment.value.multiply(0.12.toBigDecimal())
        val transactionValue = payment.value.add(depositPayment ?: BigDecimal.ZERO)

        cashControlService.updateValueForInputCash(activeCashControl, transactionValue, commission, depositPayment ?: BigDecimal.ZERO, false)

        val cashControlId: Long = activeCashControl.cashControlId

        val service = servicesService.updateServiceForPayment(payment.serviceId, transactionValue, nextPaymentDate, depositPayment)
        val customer = customerRepository.findById(service.customerId).get()
        val customerName = customer.name + if (customer.lastName != null) " " + customer.lastName else ""

        val paymentSaved = repository.save(payment)

        if (service.payDownInInstallments && depositPayment != null && depositPayment > BigDecimal.ZERO) {
            val dpPayment = ServiceDownPaymentPayment(
                service = service,
                payment = payment,
                value = depositPayment
            )
            serviceDownPaymentPaymentRepository.save(dpPayment)
        }

        val cashMovement = CashMovement(
            cashMovementType = "fee_payment",
            movementType = "IN",
            applicationUserId = payment.applicationUser.applicationUserId,
            paymentId = paymentSaved.paymentId,
            serviceId = paymentSaved.serviceId,
            value = payment.value.subtract(commission),
            description = customerName,
            cashControlId = cashControlId,
            commission = commission,
            downPayments = depositPayment,
            walletId = service.walletId,
            expenseId = null,
            revenueId = null
        )

        cashMovementRepository.save(cashMovement)

        return paymentSaved
    }

    @Transactional
    fun cancelPayment(paymentId: Long) : Payment {
        val payment = repository.findById(paymentId).get()
        if (payment.status == "canceled") {
            throw IllegalArgumentException("payment already canceled")
        }
        val activeCashControl : CashControl = cashControlService.findActiveCashControlByUser(payment.applicationUser.applicationUserId)
        val commission = payment.value.multiply(0.12.toBigDecimal())

        val downPayment = serviceDownPaymentPaymentRepository.findByPaymentPaymentId(paymentId)
        val downPaymentValue = downPayment.firstOrNull()?.value ?: BigDecimal.ZERO

        val transactionValue = payment.value.add(downPaymentValue)

        cashControlService.updateValueForInputCash(activeCashControl, transactionValue.multiply((-1).toBigDecimal()), commission.multiply((-1).toBigDecimal()), downPaymentValue.multiply((-1).toBigDecimal()), false)

        val cashControlId = activeCashControl.cashControlId

        val service = servicesService.updateServiceForPayment(payment.serviceId, transactionValue.multiply((-1).toBigDecimal()), null, downPaymentValue.multiply((-1).toBigDecimal()))
        val customer = customerRepository.findById(service.customerId).get()
        val customerName = customer.name + if (customer.lastName != null) " " + customer.lastName else ""

        payment.status = "canceled"
        repository.save(payment)

        val cashMovement = CashMovement(
            cashMovementType = "cancel_payment",
            movementType = "OUT",
            applicationUserId = payment.applicationUser.applicationUserId,
            paymentId = payment.paymentId,
            serviceId = payment.serviceId,
            value = payment.value.subtract(commission),
            description = customerName,
            cashControlId = cashControlId,
            commission = commission,
            downPayments = downPaymentValue,
            walletId = service.walletId,
            expenseId = null,
            revenueId = null
        )

        cashMovementRepository.save(cashMovement)

        return payment
    }

    fun findById(paymentId: Long): Payment {
        return repository.findById(paymentId).get()
    }

    @Transactional
    fun deletePayment(paymentId: Long) {
        repository.deleteById(paymentId)
    }

}