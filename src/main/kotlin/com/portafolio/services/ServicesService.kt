package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Service
import com.portafolio.models.ServiceSchedule
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.ProductRepository
import com.portafolio.repositories.ServiceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@org.springframework.stereotype.Service
class ServicesService {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: ServiceRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    //@Autowired
    //private lateinit var paymentScheduleRepository: PaymentScheduleRepository

    fun save(service: Service) : Service {

        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(service.applicationUserId)
        val cashControlId: Long

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = service.applicationUserId,
                active = true,
                cash = service.downPayment,
                expenses = BigDecimal.ZERO,
                revenues = service.downPayment,
                startsDate = LocalDateTime.now(),
                commission = service.downPayment,
                servicesCount = 1,
                downPayments = service.downPayment
            )

            val cashControlSaved = cashControlService.save(cashControl)

            cashControlId = cashControlSaved.cashControlId
        } else {
            cashControlService.updateValueForInputCash(activeCashControl, service.downPayment, true)

            cashControlId = activeCashControl.cashControlId
        }

        // Update product left quantity
        service.serviceProducts.forEach { p ->
            val product = productRepository.findById(p.productId).orElse(null)
            var leftQuantity = product?.leftQuantity ?: 0
            if (product != null && leftQuantity > 0) {
                product.leftQuantity = --leftQuantity
                productRepository.save(product)
            }
        }

        val serviceSaved = repository.save(service)

        val cashMovement = CashMovement(
            cashMovementType = "new_service",
            movementType = "IN",
            applicationUserId = service.applicationUserId,
            paymentId = serviceSaved.serviceId,
            value = service.downPayment,
            description = null,
            cashControlId = cashControlId,
            commission = service.downPayment
        )

        cashMovementRepository.save(cashMovement)

        /*if (service.nextPaymentDate != null) {
            val paymentSchedule = PaymentSchedule (
                serviceId = serviceSaved.serviceId,
                paymentDate = service.nextPaymentDate,
                value = service.feeValue,
                status = "pending",
                paymentNum = 1,
                note = null,
                associatedPaymentId = null,
                customerId = service.customerId
            )

            paymentScheduleRepository.save(paymentSchedule)
        }*/

        return serviceSaved
    }

    fun findAllServicesByUser(customerId: Long?) : List<Service> {
        if(customerId == null) return listOf()
        log.info("[findAllServicesByUser] Consultando informacion de los servicios del usuario, customerId: $customerId");
        val services : List<Service>? = repository.findAllServicesByUser(customerId)
        log.info("[findAllServicesByUser] Los servicios encontrados para cliente $customerId, tamaño: ${services?.size} servicios: $services");

        return if( services.isNullOrEmpty() ) listOf() else services
    }

    fun findServiceSchedule(walletIds: List<Int>) : List<ServiceSchedule>? {
        return if (walletIds.isNotEmpty())
            repository.findServicesSchedule(walletIds)
        else
            repository.findServicesSchedule()
    }
}