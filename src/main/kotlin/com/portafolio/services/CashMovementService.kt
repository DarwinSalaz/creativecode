package com.portafolio.services

import com.portafolio.dtos.ResumeWallet
import com.portafolio.dtos.UserMovementDetailDto
import com.portafolio.dtos.UserMovementsReportResponse
import com.portafolio.entities.CashMovement
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.WalletRepository
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

@Service
class CashMovementService {

    @Autowired
    lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    lateinit var walletRepository: WalletRepository

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var utilities: Utilities

    fun getCashMovementsByWallet(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : ResumeWallet {
        val movements = cashMovementRepository.getCashMovementsByWallet(walletId, startsAt.truncatedTo(ChronoUnit.DAYS), endsAt.withHour(23).withMinute(59).withSecond(59))

        val servicesCount = movements.filter { it.cashMovementType == "new_service" }.size
        val inputs = movements.filter { it.movementType == "IN" }
        val outputs = movements.filter { it.movementType == "OUT" }
        val expensesList = outputs.filter { it.cashMovementType != "cancel_payment" }
        val paymentsCanceled = outputs.filter { it.cashMovementType == "cancel_payment" }
        val paymentsCanceledValue = paymentsCanceled.map{ it.value }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val commissionsCanceledValue = paymentsCanceled.map { it.commission }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val cash = inputs.map { it.value }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}.subtract(paymentsCanceledValue)
        val downPayments = inputs.map { it.downPayments }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val commissions = inputs.map { it.commission }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}.subtract(commissionsCanceledValue)
        val expenses = expensesList.map { it.value }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}

        val walletName = walletRepository.findById(walletId)

        return ResumeWallet(
            walletMame = walletName.get().name,
            servicesCount = servicesCount,
            cash = utilities.currencyFormat(cash.toPlainString()),
            downPayments = utilities.currencyFormat(downPayments.toPlainString()),
            commissions = utilities.currencyFormat(commissions.toPlainString()),
            expenses = utilities.currencyFormat(expenses.toPlainString())
        )
    }

    fun getUserMovementsReport(applicationUserId: Long, startsAt: LocalDateTime, endsAt: LocalDateTime): UserMovementsReportResponse {
        // Obtener información del usuario
        val user = applicationUserRepository.findById(applicationUserId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado con ID: $applicationUserId")
        }
        
        val userFullName = if (user.lastName != null) {
            "${user.name} ${user.lastName}"
        } else {
            user.name
        }
        
        // Obtener movimientos en el rango de fechas (ignorando la hora)
        val startDate = startsAt.truncatedTo(ChronoUnit.DAYS)
        val endDate = endsAt.withHour(23).withMinute(59).withSecond(59)
        val movements = cashMovementRepository.findCashMovementsByUserAndDateRange(applicationUserId, startDate, endDate)
        
        // Calcular totales
        val inputs = movements.filter { it.movementType == "IN" }
        val outputs = movements.filter { it.movementType == "OUT" }
        
        val totalInputsNumber = inputs.map { it.value }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val totalOutputsNumber = outputs.map { it.value }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val totalCommissionsNumber = movements.mapNotNull { it.commission }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val totalDownPaymentsNumber = movements.mapNotNull { it.downPayments }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
        val netBalanceNumber = totalInputsNumber.subtract(totalOutputsNumber)
        
        // Formatear periodo
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val periodLabel = "${startsAt.toLocalDate().format(formatter)} / ${endsAt.toLocalDate().format(formatter)}"
        
        // Mapear movimientos a DTO detallado
        val movementsDto = movements.map { movement ->
            val walletName = if (movement.walletId != null) {
                walletRepository.findById(movement.walletId)?.get()?.name
            } else null
            
            UserMovementDetailDto(
                cashMovementId = movement.cashMovementId,
                cashMovementType = movement.cashMovementType,
                cashMovementTypeLabel = getCashMovementTypeLabel(movement.cashMovementType),
                movementType = movement.movementType,
                movementTypeLabel = if (movement.movementType == "IN") "ENTRADA" else "SALIDA",
                createdAt = movement.createdAt,
                value = utilities.currencyFormat(movement.value.toPlainString()),
                valueNumber = movement.value,
                commission = utilities.currencyFormat(movement.commission.toPlainString()),
                commissionNumber = movement.commission,
                downPayments = utilities.currencyFormat((movement.downPayments ?: BigDecimal.ZERO).toPlainString()),
                downPaymentsNumber = movement.downPayments ?: BigDecimal.ZERO,
                description = movement.description,
                justification = movement.justification,
                walletName = walletName,
                serviceId = movement.serviceId,
                paymentId = movement.paymentId,
                expenseId = movement.expenseId,
                revenueId = movement.revenueId
            )
        }
        
        return UserMovementsReportResponse(
            userFullName = userFullName,
            username = user.username,
            startsAt = startDate,
            endsAt = endDate,
            periodLabel = periodLabel,
            totalInputs = utilities.currencyFormat(totalInputsNumber.toPlainString()),
            totalInputsNumber = totalInputsNumber,
            totalOutputs = utilities.currencyFormat(totalOutputsNumber.toPlainString()),
            totalOutputsNumber = totalOutputsNumber,
            totalCommissions = utilities.currencyFormat(totalCommissionsNumber.toPlainString()),
            totalCommissionsNumber = totalCommissionsNumber,
            totalDownPayments = utilities.currencyFormat(totalDownPaymentsNumber.toPlainString()),
            totalDownPaymentsNumber = totalDownPaymentsNumber,
            netBalance = utilities.currencyFormat(netBalanceNumber.toPlainString()),
            netBalanceNumber = netBalanceNumber,
            movementsCount = movements.size,
            movements = movementsDto
        )
    }
    
    private fun getCashMovementTypeLabel(type: String): String {
        return when (type) {
            "new_service" -> "Nuevo Servicio"
            "fee_payment" -> "Abono/Cuota"
            "revenue" -> "Ingreso Adicional"
            "expense" -> "Gasto"
            "cancel_payment" -> "Cancelación de Pago"
            "delete_service" -> "Eliminación de Servicio"
            else -> type
        }
    }
}
