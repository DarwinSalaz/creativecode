package com.portafolio.controllers

import com.portafolio.dtos.RevenueDto
import com.portafolio.dtos.RevenueListResponseDto
import com.portafolio.dtos.RevenueDeleteResponseDto
import com.portafolio.mappers.RevenueMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.RevenueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE])
class RevenueController {

    @Autowired
    lateinit var mapper: RevenueMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var service: RevenueService

    @DeleteMapping("/revenue/delete/{revenue_id}")
    fun deleteRevenue(@PathVariable("revenue_id") revenueId: Long,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<RevenueDeleteResponseDto> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(RevenueDeleteResponseDto(false, "Usuario no autorizado"))

        val result = service.deleteRevenue(revenueId)

        return if (result) {
            ResponseEntity.ok(RevenueDeleteResponseDto(true, "Ingreso eliminado exitosamente"))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RevenueDeleteResponseDto(false, "No se pudo eliminar el ingreso"))
        }
    }

    @GetMapping("/revenue/list")
    fun listRevenues(
        @RequestParam("wallet_id", required = false) walletId: Int?,
        @RequestParam("start_date", required = false) startDateStr: String?,
        @RequestParam("end_date", required = false) endDateStr: String?,
        @RequestParam("revenue_type", required = false) revenueType: String?,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<List<RevenueListResponseDto>> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(emptyList())

        // Parse dates with error handling
        val startDate = try {
            startDateStr?.let { LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN) }
        } catch (e: Exception) {
            null
        }
        
        val endDate = try {
            endDateStr?.let { LocalDateTime.of(LocalDate.parse(it), LocalTime.MAX) }
        } catch (e: Exception) {
            null
        }

        val revenues = service.getRevenuesWithFilters(walletId, startDate, endDate, revenueType)

        val response = revenues.map { revenue ->
            val revenueUsername = service.getUsernameByUserId(revenue.applicationUserId)
            mapper.mapToListResponse(revenue, revenueUsername)
        }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/revenue/create")
    fun create(@Valid @RequestBody revenueDto : RevenueDto,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<Any> {
        val username: String = if (revenueDto.username.isNullOrEmpty()) {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            applicationUserService.verifyToken(token)
        } else {
            revenueDto.username!!
        }

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        revenueDto.applicationUserId = user.applicationUserId

        val expenseSaved = service.save(mapper.map(revenueDto))

        return ResponseEntity.status(HttpStatus.OK).body(expenseSaved)
    }

}