package com.portafolio.controllers

import com.portafolio.dtos.LiquidationPaymentsReportDto
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.LiquidationPaymentsReportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LiquidationPaymentsReportController(
    private val reportService: LiquidationPaymentsReportService,
    private val applicationUserService: ApplicationUserService,
    private val applicationUserRepository: ApplicationUserRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/reports/liquidation-payments")
    fun report(
        @RequestParam("wallet_id", required = false) walletId: Int?,
        @RequestParam("cash_control_id", required = false) cashControlId: Long?,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> {
        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val username = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(username)

        if (user == null || user.userProfileId != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to "Operación exclusiva para administradores"))
        }

        return try {
            ResponseEntity.ok(reportService.generate(walletId, cashControlId))
        } catch (error: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (error.message ?: "Solicitud inválida")))
        } catch (error: DataAccessException) {
            log.error("[liquidation-payments-report] Database error for walletId=$walletId, cashControlId=$cashControlId", error)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("message" to "No fue posible consultar los datos del informe en este momento."))
        } catch (error: Exception) {
            log.error("[liquidation-payments-report] Unexpected error for walletId=$walletId, cashControlId=$cashControlId", error)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Ocurrió un error procesando el informe."))
        }
    }
}
