package com.portafolio.mappers

import com.portafolio.dtos.PaymentDto
import com.portafolio.entities.Payment
import org.springframework.stereotype.Component

@Component
class PaymentMapper {

    fun map(paymentDto: PaymentDto) =
        Payment(
            applicationUserId = paymentDto.applicationUserId,
            serviceId = paymentDto.serviceId,
            value = paymentDto.value
        )

}