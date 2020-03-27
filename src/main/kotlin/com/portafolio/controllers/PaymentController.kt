package com.portafolio.controllers

import com.portafolio.dtos.PaymentDto
import com.portafolio.entities.Payment
import com.portafolio.mappers.PaymentMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class PaymentController {

    @Autowired
    lateinit var service: PaymentService

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var mapper: PaymentMapper

    @PostMapping("/payment/create")
    fun registerPayment(@Valid @RequestBody paymentDto : PaymentDto,
                        @RequestHeader("Authorization") authorization: String) : Payment? {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        paymentDto.applicationUserId = user.applicationUserId

        val payment = mapper.map(paymentDto)

        return service.save(payment, paymentDto.nextPaymentDate)
    }

}