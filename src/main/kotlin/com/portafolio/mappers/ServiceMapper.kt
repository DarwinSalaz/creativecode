package com.portafolio.mappers

import java.math.BigDecimal
import com.portafolio.dtos.*
import com.portafolio.entities.Service
import com.portafolio.entities.ServiceProduct
import com.portafolio.models.*
import com.portafolio.repositories.PaymentRepository
import com.portafolio.repositories.ProductRepository
import com.portafolio.services.Utilities
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class ServiceMapper {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var utilities: Utilities

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    fun map(serviceDto: ServiceDto, applicationUserId: Long): Service {
        val service = Service(
            applicationUserId = applicationUserId,
            serviceValue = serviceDto.serviceValue,
            downPayment = serviceDto.downPayment,
            discount = serviceDto.discount,
            debt = serviceDto.debt,
            totalValue = serviceDto.totalValue,
            walletId = serviceDto.walletId,
            hasProducts = serviceDto.serviceProducts.isNotEmpty(),
            customerId = serviceDto.customerId,
            state = serviceDto.state,
            daysPerFee = serviceDto.daysPerFee,
            quantityOfFees = serviceDto.quantityOfFees,
            feeValue = serviceDto.feeValue,
            observations = serviceDto.observations,
            nextPaymentDate = serviceDto.nextPaymentDate,
            pendingFees = serviceDto.pendingFees
        )

        service.serviceProducts = serviceDto.serviceProducts
            .map {
                ServiceProduct(productId = it.productId, service = service, value = it.value, quantity = it.quantity)
            }.toMutableSet()

        return service
    }

    fun mapReverse(service: Service): ServiceDto {
        val serviceDto = ServiceDto(
            serviceId = service.serviceId,
            applicationUserId = service.applicationUserId,
            serviceValue = service.serviceValue,
            downPayment = service.downPayment,
            discount = service.discount,
            debt = service.debt,
            totalValue = service.totalValue,
            walletId = service.walletId,
            customerId = service.customerId,
            state = service.state,
            daysPerFee = service.daysPerFee,
            quantityOfFees = service.quantityOfFees,
            feeValue = service.feeValue,
            observations = service.observations,
            pendingFees = service.pendingFees
        )

        serviceDto.serviceProducts = service.serviceProducts
            .map {
                ServiceProductDto(productId = it.productId, value = it.value, quantity = it.quantity)
            }.toMutableList()

        return serviceDto
    }

    fun mapServicesByUser(service: Service): ServicesByCustomerResponse {

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val createdAt = service.createdAt.toLocalDate().format(formatter)
        val nextPaymentDate = service.nextPaymentDate?.toLocalDate()?.format(formatter)

        log.info("[mapServicesByUser] Mapping service: $service")

        val servicesByCustomerResponse = ServicesByCustomerResponse(
            serviceId = service.serviceId,
            applicationUserId = service.applicationUserId,
            serviceValue = utilities.currencyFormat(service.serviceValue.toString()),
            downPayment = utilities.currencyFormat(service.downPayment.toString()),
            discount = utilities.currencyFormat(service.discount.toString()),
            debt = utilities.currencyFormat(service.debt.toString()),
            totalValue = utilities.currencyFormat(service.totalValue.toString()),
            walletId = service.walletId,
            state = service.state,
            customerId = service.customerId,
            createdAt = createdAt,
            feeValue = utilities.currencyFormat(service.feeValue.toString()),
            totalValueNumber = service.totalValue,
            downPaymentNumber = service.downPayment,
            quantityOfFees = service.quantityOfFees,
            daysPerFee = service.daysPerFee,
            debtInNumber = service.debt,
            observations = service.observations,
            pendingValue = service.pendingValue?.let { utilities.currencyFormat(service.pendingValue.toString()) },
            pendingFees = service.pendingFees,
            nextPaymentDate = nextPaymentDate,
            markedForWithdrawal = service.markedForWithdrawal
        )

        servicesByCustomerResponse.serviceProducts = service.serviceProducts
            .filter { it.enabled }
            .map {
                val product = productRepository.findById(it.productId).orElse(null)
                ServiceProductDto(productId = it.productId, value = it.value, quantity = it.quantity, name = product?.name)
            }.toMutableList()

        servicesByCustomerResponse.payments = paymentRepository
            .findAllPaymentsByServiceId(serviceId = service.serviceId)?.
            filter { it.status != "canceled" }?.
            map { payment ->
                PaymentResumeDto(
                    paymentId = payment.paymentId,
                    value = utilities.currencyFormat(payment.value.toPlainString()),
                    username = payment.applicationUser.username,
                    createdAt = payment.createdAt.toLocalDate().format(formatter)
                )} ?: emptyList()

        return servicesByCustomerResponse
    }

    fun map(serviceSchedules: List<ServiceSchedule>) =
        serviceSchedules.map { it ->
            ServiceScheduleResponse (
                customerId = it.customerId,
                name = it.name,
                lastName = it.lastName,
                icon = it.icon,
                feeValue = utilities.currencyFormat(it.feeValue.toString()),
                nextPaymentDate = it.nextPaymentDate?.toLocalDate()
            )
        }

    fun mapReport(servicesReportInt: List<ServiceReportInt>): ServiceReportResponse {
        val totalProductValues = servicesReportInt.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.product_values) }
        val totalDiscount = servicesReportInt.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.discount) }
        val totalServiceValue = servicesReportInt.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.service_value) }
        val totalDebt = servicesReportInt.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.debt) }
        val servicesData = servicesReportInt.map {
            ServiceReport(
                id = it.id,
                client = it.client,
                products = it.products,
                productValues = utilities.currencyFormat(it.product_values.toString()),
                discount = utilities.currencyFormat(it.discount.toString()),
                serviceValue = utilities.currencyFormat(it.service_value.toString()),
                debt = utilities.currencyFormat(it.debt.toString()),
                wallet = it.wallet,
                username = it.username,
                createdAt = it.created_at
            )
        }

        return ServiceReportResponse(
            totalProductValues = utilities.currencyFormat(totalProductValues.toString()),
            totalDiscount = utilities.currencyFormat(totalDiscount.toString()),
            totalServiceValue = utilities.currencyFormat(totalServiceValue.toString()),
            totalDebt = utilities.currencyFormat(totalDebt.toString()),
            servicesData = servicesData
        )
    }


    fun mapPaymentReport(paymentReportInterfaces: List<PaymentReportInterface>): PaymentReportResponse {
        val totalValue = paymentReportInterfaces.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.value) }

        val payments = paymentReportInterfaces.map {
            PaymentReport(
                id = it.id,
                client = it.client,
                serviceId = it.service_id,
                value = utilities.currencyFormat(it.value.toString()),
                wallet = it.wallet,
                username = it.username,
                createdAt = it.created_at
            )
        }

        return PaymentReportResponse(
            totalValue = utilities.currencyFormat(totalValue.toString()),
            paymentsData = payments
        )
    }

    fun mapExpiredServicesReport(expiredServiceReportInterface: List<ExpiredServiceReportInterface>): ExpiredServiceReportResponse {
        val totalValue = expiredServiceReportInterface.fold(BigDecimal.ZERO) { sum, element -> sum.add(element.debt) }

        val services = expiredServiceReportInterface.map {
            ExpiredServiceReport(
                client = it.client,
                cellphone = it.cellphone,
                address = it.address,
                totalValue = utilities.currencyFormat(it.total_value.toString()),
                debt = utilities.currencyFormat(it.debt.toString()),
                pendingFees = it.pending_fees,
                nextPaymentDate = it.next_payment_date
            )
        }

        return ExpiredServiceReportResponse(
            totalValue = utilities.currencyFormat(totalValue.toString()),
            expiredServices = services
        )
    }

}


