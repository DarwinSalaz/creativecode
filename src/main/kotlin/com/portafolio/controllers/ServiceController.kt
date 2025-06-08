package com.portafolio.controllers

import com.portafolio.dtos.*
import com.portafolio.mappers.ServiceMapper
import com.portafolio.models.*
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.repositories.ServiceRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.ServicesService
import com.portafolio.services.Utilities
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
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
    lateinit var repository: ServiceRepository

    @Autowired
    lateinit var utilities: Utilities

    @PostMapping("/service/create")
    fun createService(@Valid @RequestBody serviceDto: ServiceDto,
                      @RequestHeader("Authorization") authorization: String) : ResponseEntity<Any> {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        val serviceCreated = service.save(mapper.map(serviceDto, user.applicationUserId), user, serviceDto.initialPayment)

        return ResponseEntity.status(HttpStatus.OK).body(serviceCreated)
    }

    @PutMapping("/service/update")
    fun updateService(@Valid @RequestBody serviceDto: ServiceUpdateDto,
                      @RequestHeader("Authorization") authorization: String) : ResponseEntity<Any> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        service.updateService(serviceDto)

        return ResponseEntity(mapOf("code" to "ok", "message" to "success"), HttpStatus.OK)
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

        if (services != null) {
            services = if (walletRequest.expiredServices == true)
                getExpiredServicesByDate(services, walletRequest.date ?: LocalDateTime.now())
            else
                getServicesByDate(services, walletRequest.date ?: LocalDateTime.now())
        }

        return if (services != null) mapper.map(services) else listOf()
    }

    @PostMapping("/service/expired_services")
    fun getExpiredServices(
        @Valid @RequestBody walletRequest: WalletRequest
    ) : List<ServiceScheduleResponse>? {
        var services = service.findServiceSchedule(walletRequest.walletIds ?: listOf())

        if (services != null) {
            services = getExpiredServicesByDate(services, walletRequest.date ?: LocalDateTime.now())
        }

        return if (services != null) mapper.map(services) else listOf()
    }

    fun getServicesByDate(services: List<ServiceSchedule>, date: LocalDateTime) = services
        .filter { it.nextPaymentDate != null }
        .filter {
            it.nextPaymentDate!!.truncatedTo(ChronoUnit.DAYS) == date.truncatedTo(ChronoUnit.DAYS)
        }

    fun getExpiredServicesByDate(services: List<ServiceSchedule>, date: LocalDateTime) = services
        .filter { it.nextPaymentDate != null }
        .filter {
            it.hasExpiredPayment == true || it.nextPaymentDate!!.truncatedTo(ChronoUnit.DAYS) < date.truncatedTo(ChronoUnit.DAYS)
        }

    @PostMapping("/service/cancel_service")
    fun cancelService(
        @Valid @RequestBody cancelServiceRequest: CancelServiceRequest,
        @RequestHeader("Authorization") authorization: String): ResponseEntity<Map<String, String>>
    {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return ResponseEntity(mapOf("code" to "error", "message" to "fail"), HttpStatus.BAD_REQUEST)

        service.cancelService(cancelServiceRequest, user.applicationUserId)

        return ResponseEntity(mapOf("code" to "ok", "message" to "success"), HttpStatus.OK)
    }

    @PostMapping("service/report")
    fun reportService(@Valid @RequestBody request: ResumeWalletRequest): ServiceReportResponse {

        val data = repository.reportService(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))

        return mapper.mapReport(data)
    }

    @PostMapping("payment/report")
    fun reportPayments(@Valid @RequestBody request: ResumeWalletRequest): PaymentReportResponse {

        val data = repository.reportPayments(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))

        return mapper.mapPaymentReport(data)
    }

    @PostMapping("expired/report")
    fun reportExpiredServices(@Valid @RequestBody request: ResumeWalletRequest): ExpiredServiceReportResponse {
        val data = repository.reportExpiredServices(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))

        return mapper.mapExpiredServicesReport(data)
    }

    @PostMapping("withdrawal/report")
    fun reportWithdrawalServices(@Valid @RequestBody request: ResumeWalletRequest): ExpiredServiceReportResponse {
        val data = repository.reportMarkedForWithdrawalServices(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))

        return mapper.mapExpiredServicesReport(data)
    }

    @PostMapping("canceled/report")
    fun reportCanceledServices(@Valid @RequestBody request: ResumeWalletRequest): ExpiredServiceReportResponse {
        val data = repository.reportCanceledServices(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))

        return mapper.mapExpiredServicesReport(data)
    }

    @PostMapping("/wallet-resume", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getWalletResume(@RequestBody request: ResumeWalletRequest): ResponseEntity<WalletResumeResponse> {
        val response = service.generateWalletReportData(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/mark-for-withdrawal")
    fun markForWithdrawal(@RequestBody request: markServiceDTO): ResponseEntity<Map<String, String>> {
        service.markForWithdrawal(request.serviceId, request.customerId)
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

    @PostMapping("/mark-as-lost")
    fun markAsLost(@RequestBody request: markServiceDTO): ResponseEntity<Map<String, String>> {
        service.markAsLost(request.serviceId, request.customerId)
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

}