package com.portafolio.controllers

import com.portafolio.dtos.PaymentDto
import com.portafolio.dtos.PaymentReceiptResponse
import com.portafolio.mappers.PaymentMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.PaymentService
import com.portafolio.services.ServicesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class PaymentController {

    @Autowired
    lateinit var service: PaymentService

    @Autowired
    lateinit var servicesService: ServicesService

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var mapper: PaymentMapper

    @PostMapping("/payment/create")
    fun registerPayment(@Valid @RequestBody paymentDto : PaymentDto,
                        @RequestHeader("Authorization") authorization: String) : PaymentReceiptResponse? {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        /*if (paymentDto.value.compareTo(BigDecimal.ZERO) == 0) {
            servicesService.updateNextPaymentDate(paymentDto.nextPaymentDate, paymentDto.serviceId)
            return mapOf("payment_id" to 0.toLong())
        }*/

        paymentDto.applicationUserId = user.applicationUserId

        return service.save(mapper.map(paymentDto), paymentDto.nextPaymentDate, paymentDto.depositPayment)
    }

    @PutMapping("/payment/cancel")
    fun cancelPayment(@RequestParam("payment_id") paymentId: Long,
                        @RequestHeader("Authorization") authorization: String) : ResponseEntity<Map<String, String>> {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("ok" to "false", "message" to "UNAUTHORIZED"))
        if (user.userProfileId != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("ok" to "false", "message" to "Solo un administrador puede cancelar abonos."))
        }

        try {
            service.cancelPayment(paymentId, user)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest()
                .body(mapOf("ok" to "false", "message" to (e.message ?: "No fue posible cancelar el abono.")))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("ok" to "false", "message" to (e.message ?: "Error interno cancelando el abono.")))
        }

        return ResponseEntity.ok(mapOf("ok" to "true"))
    }

}
