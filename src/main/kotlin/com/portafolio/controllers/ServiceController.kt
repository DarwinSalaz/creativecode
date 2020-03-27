package com.portafolio.controllers

import com.portafolio.dtos.ServiceDto
import com.portafolio.dtos.ServicesByCustomerResponse
import com.portafolio.entities.Service
import com.portafolio.mappers.ServiceMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.ServicesService
import io.jsonwebtoken.JwtException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ServiceController {
    @Autowired
    lateinit var service: ServicesService

    @Autowired
    lateinit var mapper: ServiceMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

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

    @GetMapping("/services_by_customer")
    fun findAllServicesByUser(
        @RequestHeader("customer_id") customerId: Long?
    ) : List<ServicesByCustomerResponse> {

        val services = service.findAllServicesByUser(customerId)

        return services.map { mapper.mapServicesByUser(it) }.toMutableList()
    }
}