package com.portafolio.mappers

import com.portafolio.dtos.PaymentDto
import com.portafolio.entities.Payment
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PaymentMapper {

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    fun map(paymentDto: PaymentDto) =
        Payment(
            applicationUser = applicationUserRepository.getOne(paymentDto.applicationUserId),
            serviceId = paymentDto.serviceId,
            value = paymentDto.value
        )

}