package com.portafolio.controllers

import com.portafolio.dtos.ServiceDto
import com.portafolio.dtos.ServiceScheduleResponse
import com.portafolio.dtos.ServicesByCustomerResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.entities.Service
import com.portafolio.mappers.ServiceMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.ServicesService
import com.portafolio.services.Utilities
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.temporal.ChronoUnit
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ServiceController {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var service: ServicesService

    @Autowired
    lateinit var mapper: ServiceMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var utilities: Utilities

    @PostMapping("/service/create")
    fun createService(@Valid @RequestBody serviceDto: ServiceDto,
                      @RequestHeader("Authorization") authorization: String) : Service? {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        return service.save(mapper.map(serviceDto, user.applicationUserId))
    }

    @ExceptionHandler(JwtException::class)
    fun handleConstraintViolation(ex: JwtException): ResponseEntity<Map<String, String>> {

        return ResponseEntity(mapOf("code" to "invalid_input", "message" to ex.message!!), HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/service/services_by_customer")
    fun findAllServicesByUser(
        @RequestHeader("customerid") customerId: Long?
    ) : List<ServicesByCustomerResponse> {
        log.info("[findAllServicesByUser] Controller - se consultan los servicios del usuario, customerId: $customerId")

        val services = service.findAllServicesByUser(customerId)

        return services.map { mapper.mapServicesByUser(it) }.toMutableList()
    }

    @PostMapping("/service/services_schedule")
    fun findServicesSchedule(
        @Valid @RequestBody walletRequest: WalletRequest
    ) : List<ServiceScheduleResponse>? {

        /*try {
            date = if (dateStr != null) LocalDateTime.parse(dateStr, formatter) else null
        } catch (e: Exception) {
            log.error("Invalid date format for {}", dateStr, e)
        }*/

        var services = service.findServiceSchedule(walletRequest.walletIds ?: listOf())

        if (walletRequest.date != null && services != null) {
            services = services
                .filter { it.nextPaymentDate != null }
                .filter {
                    it.nextPaymentDate!!.truncatedTo(ChronoUnit.DAYS) == walletRequest.date.truncatedTo(ChronoUnit.DAYS)
                }
        }

        return if (services != null) mapper.map(services) else listOf()
    }

}