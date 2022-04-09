package com.portafolio.mappers

import com.portafolio.dtos.*
import com.portafolio.entities.Service
import com.portafolio.entities.ServiceProduct
import com.portafolio.models.ServiceSchedule
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
            nextPaymentDate = serviceDto.nextPaymentDate
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
            observations = service.observations
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
            quantityOfFees = service.quantityOfFees,
            daysPerFee = service.daysPerFee,
            debtInNumber = service.debt,
            observations = service.observations,
            pendingValue = service.pendingValue?.let { utilities.currencyFormat(service.pendingValue.toString()) }
        )

        servicesByCustomerResponse.serviceProducts = service.serviceProducts
            .filter { it.enabled }
            .map {
                val product = productRepository.findById(it.productId).orElse(null)
                ServiceProductDto(productId = it.productId, value = it.value, quantity = it.quantity, name = product?.name)
            }.toMutableList()

        servicesByCustomerResponse.payments = paymentRepository
            .findAllPaymentsByServiceId(serviceId = service.serviceId)?.map { payment ->
                PaymentResumeDto(
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
}