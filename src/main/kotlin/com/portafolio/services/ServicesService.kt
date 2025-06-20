package com.portafolio.services

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.portafolio.dtos.*
import com.portafolio.entities.*
import com.portafolio.models.ServiceSchedule
import com.portafolio.repositories.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.transaction.Transactional

@org.springframework.stereotype.Service
class ServicesService(
    val expenseRepository: ExpenseRepository,
    val revenueRepository: RevenueRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: ServiceRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var logCancelServiceRepository: LogCancelServiceRepository

    @Autowired
    private lateinit var serviceDownPaymentPaymentRepository: ServiceDownPaymentPaymentRepository

    @Autowired
    private lateinit var paymentService: PaymentService

    //@Autowired
    //private lateinit var paymentScheduleRepository: PaymentScheduleRepository

    @Transactional
    fun save(service: Service, applicationUser: ApplicationUser, initialPayment: BigDecimal?) : Service {

        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(service.applicationUserId)
        val cashControlId: Long

        var commission = BigDecimal.ZERO
        if (!service.payDownInInstallments) {
            commission = initialPayment?.multiply(0.12.toBigDecimal())?.setScale(2) ?: BigDecimal.ZERO
        }

        // Esto pasa cuando es una compra de contado
        if (service.debt.compareTo(BigDecimal.ZERO) == 0) {
            commission = BigDecimal.ZERO
            service.state = "fully_paid"
            service.pendingFees = 0
        }

        val deposit = initialPayment?.subtract(commission)?.setScale(2) ?: BigDecimal.ZERO
        val transactionValue = service.downPayment.add(initialPayment ?: BigDecimal.ZERO)

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = service.applicationUserId,
                active = true,
                cash = BigDecimal.ZERO.add(deposit), // saldo de abono descontando la comision
                expenses = BigDecimal.ZERO,
                revenues = transactionValue, // seña  + abono
                startsDate = LocalDateTime.now(),
                commission = commission,
                servicesCount = 1,
                downPayments = service.downPayment // seña
            )

            val cashControlSaved = cashControlService.save(cashControl)

            cashControlId = cashControlSaved.cashControlId
        } else {
            cashControlService.updateValueForInputCash(activeCashControl, transactionValue, commission, service.downPayment, true)

            cashControlId = activeCashControl.cashControlId
        }

        // Update product left quantity
        service.serviceProducts.forEach { p ->
            val product = productRepository.findById(p.productId).orElse(null)
            val leftQuantity = product?.leftQuantity ?: 0
            if (product != null && leftQuantity > 0 && p.quantity > 0) {
                product.leftQuantity = leftQuantity - p.quantity
                productRepository.save(product)
            }
        }

        val serviceSaved = repository.save(service)

        val payment = if (initialPayment != null && initialPayment >= BigDecimal.ZERO) {
            paymentService.savePayment(serviceSaved.serviceId, initialPayment, applicationUser)
        } else null

        val customer = customerRepository.findById(serviceSaved.customerId).get()

        val cashMovement = CashMovement(
            cashMovementType = "new_service",
            movementType = "IN",
            applicationUserId = service.applicationUserId,
            paymentId = payment?.paymentId,
            serviceId = serviceSaved.serviceId,
            value = deposit ?: BigDecimal.ZERO,
            description = customer.name + if (customer.lastName != null) " " + customer.lastName else "",
            cashControlId = cashControlId,
            commission = commission,
            downPayments = service.downPayment,
            walletId = service.walletId,
            expenseId = null,
            revenueId = null
        )

        cashMovementRepository.save(cashMovement)


        if (service.payDownInInstallments && service.downPayment > BigDecimal.ZERO && payment != null) {
            val dpPayment = ServiceDownPaymentPayment(
                service = serviceSaved,
                payment = payment,
                value = service.downPayment
            )
            serviceDownPaymentPaymentRepository.save(dpPayment)
        }


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
        else {
            val services = repository.findServicesSchedule()

            /*services?.groupBy { it.customerId }?.map {
                    ServiceSchedule(
                        customerId = it.key,
                        name = it.value[0].name,
                        lastName = it.value[0].lastName,
                        icon = it.value[0].icon,
                        feeValue = it.value.map { s -> s.feeValue }.fold (BigDecimal.ZERO) { a, b -> a.add(b) },
                        nextPaymentDate = it.value[0].nextPaymentDate
                    )
                }*/
            services

        }

    }

    @Transactional
    fun updateService(updateServiceDto: ServiceUpdateDto) {
        val service = repository.findById(updateServiceDto.serviceId).get()

        service.quantityOfFees = updateServiceDto.quantityOfFees
        service.feeValue = updateServiceDto.feeValue
        service.pendingFees = service.debt.divide(service.feeValue, 0, RoundingMode.CEILING).toInt()

        repository.save(service)
    }

    @Transactional
    fun cancelService(cancelServiceRequest: CancelServiceRequest, applicationUserId: Long) {
        val service = repository.findById(cancelServiceRequest.serviceId).get()
        val serviceProducts = service.serviceProducts
        val productIdsToCancel = cancelServiceRequest.productIds

        serviceProducts.filter { productIdsToCancel.contains(it.productId) }
            .forEach { it.enabled = false }

        // Update product left quantity
        productIdsToCancel.forEach { p ->
            val product = productRepository.findById(p).orElse(null)
            var leftQuantity = product?.leftQuantity ?: 0
            if (product != null) {
                product.leftQuantity = ++leftQuantity
                productRepository.save(product)
            }
        }

        service.debt = service.debt.subtract(cancelServiceRequest.discount)
        service.discount = service.discount.add(cancelServiceRequest.discount)
        service.totalValue = service.totalValue.subtract(cancelServiceRequest.discount)
        service.observations = if(service.observations != null)
            service.observations + "\n - Proceso de cancelación de productos aplicado a este servicio"
        else null

        if (serviceProducts.none { it.enabled } || service.debt.compareTo(BigDecimal.ZERO) == 0) {
            service.state = "canceled"
        }

        repository.save(service)

        saveLogCancelService(cancelServiceRequest, applicationUserId, service.state == "canceled")
    }

    fun saveLogCancelService(cancelServiceRequest: CancelServiceRequest, applicationUserId: Long, completeCancellation: Boolean) {
        val logCancelService = LogCancelService(
            applicationUserId = applicationUserId,
            serviceId = cancelServiceRequest.serviceId,
            productIds = cancelServiceRequest.productIds.toString(),
            completeCancellation = completeCancellation
        )

        logCancelServiceRepository.save(logCancelService)
    }

    @Transactional
    fun updateServiceForPayment(serviceId: Long, value: BigDecimal, nextPaymentDate: LocalDateTime?) : Service {
        val service = repository.findById(serviceId).get()

        service.debt = service.debt - value
        service.pendingFees = service.debt.divide(service.feeValue, 0, RoundingMode.CEILING).toInt()
        service.nextPaymentDate = nextPaymentDate ?: service.nextPaymentDate
        service.state = if (service.debt.compareTo(BigDecimal.ZERO) == 0) "fully_paid" else "paying"

        service.pendingValue = getPendingValue(value, service.pendingValue, service.feeValue, service.state)


        if (service.hasExpiredPayment == true && value.compareTo(service.feeValue) >= 0) {
            service.hasExpiredPayment = false
        } else if (service.hasExpiredPayment != true && value.compareTo(BigDecimal.ZERO) == 0) {
            service.hasExpiredPayment = true
        }

        return repository.save(service)
    }

    fun generateWalletReportData(dto: ResumeWalletRequest): WalletResumeResponse {
        val expenses = expenseRepository
            .findByWalletAndDateRange(dto.walletId, dto.startsAt, dto.endsAt)

        val revenues = revenueRepository
            .findByWalletAndDateRange(dto.walletId, dto.startsAt, dto.endsAt)

        val wallet = walletRepository.findById(dto.walletId)
            .orElseThrow { IllegalArgumentException("Wallet not found: ${dto.walletId}") }

        val incomeData = revenues.map {
            WalletTransactionSummary(
                type = "income",
                date = it.revenueDate,
                category = it.revenueType,
                value = it.value,
                justification = it.justification
            )
        }

        val expenseData = expenses.map {
            WalletTransactionSummary(
                type = "expense",
                date = it.expenseDate,
                category = it.expenseType,
                value = it.value,
                justification = it.justification
            )
        }

        val totalIncome = incomeData.fold(BigDecimal.ZERO) { acc, it -> acc + it.value }
        val totalExpense = expenseData.fold(BigDecimal.ZERO) { acc, it -> acc + it.value }
        val finalBalance = totalIncome - totalExpense

        return WalletResumeResponse(
            walletName = wallet.name,
            startsAt = dto.startsAt,
            endsAt = dto.endsAt,
            incomes = incomeData,
            expenses = expenseData,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance
        )
    }

    @Transactional
    fun updateNextPaymentDate(nextPaymentDate: LocalDateTime?, serviceId: Long) {
        repository.updateNextPaymentDateService(nextPaymentDate, serviceId)
    }

    fun getPendingValue(value: BigDecimal, pendingValue: BigDecimal?, feeValue: BigDecimal, state: String): BigDecimal? {
        val hasPendingValue = pendingValue != null
        val isPendingValue = hasPendingValue && ( value.compareTo(pendingValue) == 0 || pendingValue!!.add(feeValue).compareTo(value) == 0 )

        return if (!hasPendingValue && value < feeValue) {
            feeValue - value
        } else if (state == "fully_paid" || isPendingValue) {
            null
        } else if (hasPendingValue && value < pendingValue) {
            pendingValue!!.subtract(value)
        } else if (hasPendingValue && value > pendingValue && value < feeValue) {
            feeValue.subtract(value.subtract(pendingValue))
        } else if (hasPendingValue && value > feeValue && value < feeValue.add(pendingValue)) {
            feeValue.add(pendingValue).subtract(value)
        } else if (hasPendingValue && value > feeValue.add(pendingValue)) {
            null
        } else {
            pendingValue
        }
    }

    @Transactional
    fun markForWithdrawal(serviceId: Long, customerId: Long) {
        val service = repository.findById(serviceId).orElseThrow {
            IllegalArgumentException("Servicio no encontrado")
        }

        val customer = customerRepository.findById(customerId).orElseThrow {
            IllegalArgumentException("Cliente no encontrado")
        }

        service.markedForWithdrawal = true
        customer.blocked = true

        repository.save(service)
        customerRepository.save(customer)
    }

    @Transactional
    fun markAsLost(serviceId: Long, customerId: Long) {
        val service = repository.findById(serviceId).orElseThrow {
            IllegalArgumentException("Servicio no encontrado")
        }

        val customer = customerRepository.findById(customerId).orElseThrow {
            IllegalArgumentException("Cliente no encontrado")
        }

        service.markedAsLost = true
        customer.blocked = true

        repository.save(service)
        customerRepository.save(customer)
    }

    fun isCustomerInDebit(customerId: Long): Any {
        return repository.hasOverdueServices(customerId)
    }
}