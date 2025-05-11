package com.portafolio.services

import com.portafolio.dtos.CashControlClosureRequest
import com.portafolio.dtos.CashControlResponse
import com.portafolio.dtos.CashMovementDto
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.CashControl
import com.portafolio.mappers.CashControlMapper
import com.portafolio.repositories.CashControlRepository
import com.portafolio.repositories.CashMovementRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.transaction.Transactional

@Service
class CashControlService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: CashControlRepository

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    private lateinit var mapper: CashControlMapper

    @Autowired
    lateinit var utilities: Utilities

    fun save(cashControl: CashControl) : CashControl {

        return repository.save(cashControl)
    }

    @Transactional
    fun updateValueForInputCash(cashControl: CashControl, transactionValue: BigDecimal, commissionTransaction: BigDecimal, downPayment: BigDecimal, isNewService: Boolean) {
        // efectivo: saldo actual del usuario (para retornar a la empresa) valor actual + (valor pagado - comision - seña)
        val cash = cashControl.cash.add(transactionValue.subtract(commissionTransaction).subtract(downPayment))
        // ingresos: saldo actual + valor pagado
        val revenues = cashControl.revenues.add(transactionValue)
        val expenses = cashControl.expenses
        val servicesCount = if (isNewService) cashControl.servicesCount + 1 else cashControl.servicesCount
        // comisiones: saldo actual + comision del abono
        val commission = cashControl.commission.add(commissionTransaction)
        // señas: saldo actual + seña
        val downPayments = cashControl.downPayments?.add(downPayment) ?: BigDecimal.ZERO

        return repository.updateCashControlValues(cash, revenues, expenses, commission, servicesCount, downPayments, cashControl.cashControlId)
    }

    @Transactional
    fun updateValuesForExpenses(cashControl: CashControl, value: BigDecimal) {
        val cash = cashControl.cash.subtract(value)
        val revenues = cashControl.revenues
        val expenses = cashControl.expenses.add(value)
        val servicesCount = cashControl.servicesCount
        val downPayments = cashControl.downPayments ?: BigDecimal.ZERO
        val commissions = cashControl.commission

        return repository.updateCashControlValues(cash, revenues, expenses, commissions, servicesCount, downPayments, cashControl.cashControlId)
    }

    @Transactional
    fun updateValuesForDeleteExpenses(cashControl: CashControl, value: BigDecimal) {
        val cash = cashControl.cash.add(value)
        val revenues = cashControl.revenues
        val expenses = cashControl.expenses.subtract(value)
        val servicesCount = cashControl.servicesCount
        val downPayments = cashControl.downPayments ?: BigDecimal.ZERO
        val commissions = cashControl.commission

        return repository.updateCashControlValues(cash, revenues, expenses, commissions, servicesCount, downPayments, cashControl.cashControlId)
    }

    fun findHistoryCashControlByUser(applicationUserId : Long) = repository.findHistoryCashControlByUser(applicationUserId)

    fun findActiveCashControlByUser(applicationUserId : Long) : CashControl {

        var cashControl = repository.findActiveCashControlByUser(applicationUserId)

        if (cashControl == null) {
            cashControl = CashControl(
                applicationUserId = applicationUserId,
                active = true,
                cash = BigDecimal.ZERO,
                expenses = BigDecimal.ZERO,
                revenues = BigDecimal.ZERO,
                commission = BigDecimal.ZERO,
                downPayments = BigDecimal.ZERO,
                startsDate = LocalDateTime.now(),
                servicesCount = 0
            )

            repository.save(cashControl)
        }

        return cashControl
    }

    fun closureAccount(request: CashControlClosureRequest, userClosure: String) : CashControl? {
        val cashControl = repository.findById(request.cashControlId).orElse(null)

        if (cashControl == null || !cashControl.active) return null

        cashControl.active = false
        cashControl.endsDate = LocalDateTime.now()
        cashControl.closureDate = LocalDateTime.now()
        cashControl.closureUser = userClosure
        cashControl.closureValueReceived = request.closureValueReceived
        cashControl.closureNotes = request.closureNotes

        return repository.save(cashControl)
    }

    fun getCashControlMovements(cashControlId: Long) : List<CashMovementDto> {
        val movements = cashMovementRepository.findCashMovementsByCashControlId(cashControlId)

        return mapper.mapCashMovements(movements)
    }

    fun getDailyCashControl(applicationUser : ApplicationUser) : CashControlResponse {
        val cashControl = findActiveCashControlByUser(applicationUser.applicationUserId)

        var movements = cashMovementRepository.findCashMovementsByCashControlId(cashControl!!.cashControlId)

        movements = movements
            .filter {
                it.createdAt.truncatedTo(ChronoUnit.DAYS) == LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
            }


        val movementsDto = mapper.mapCashMovements(movements)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startsDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val endsDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
        val period = startsDate.toLocalDate().format(formatter) + "/" + endsDate.toLocalDate().format(formatter)
        var inputs = movements.filter { it.movementType == "IN" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val cancelPayments = movements.filter { it.movementType == "OUT" && it.cashMovementType == "cancel_payment" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        inputs = inputs.subtract(cancelPayments)

        val expenses = movements.filter { it.movementType == "OUT" && it.cashMovementType != "cancel_payment" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }


        var commissions = movements.filter { it.movementType == "IN" }.mapNotNull { it.commission }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val commissionsCanceled = movements.filter { it.movementType == "OUT" && it.cashMovementType == "cancel_payment" }.map { it.commission }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        commissions = commissions.subtract(commissionsCanceled)

        val downPayments = movements.filter { it.movementType == "IN" && it.cashMovementType == "new_service" }.map { it.downPayments }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val cash = inputs.subtract(expenses)
        val revenues = inputs.add(commissions).add(downPayments)

        return CashControlResponse (
            fullName = if (applicationUser.lastName == null) applicationUser.name else applicationUser.name + " " + applicationUser.lastName,
            cashControlId = cashControl.cashControlId,
            applicationUserId = applicationUser.applicationUserId,
            startsDate = startsDate,
            endsDate = endsDate,
            revenues = utilities.currencyFormat(revenues.toString()),
            expenses = utilities.currencyFormat(expenses.toString()),
            cash = utilities.currencyFormat(cash.toString()),
            active = cashControl.active,
            period = period,
            servicesCount = movements.filter { it.movementType == "IN" && it.cashMovementType == "new_service" }.size,
            cashNumber = cash,
            commission = utilities.currencyFormat(commissions.toString()),
            commissionNumber = commissions ?: BigDecimal.ZERO,
            downPayments = utilities.currencyFormat(downPayments.toString()),
            downPaymentsNumber = downPayments ?: BigDecimal.ZERO,
            movements = movementsDto
        )
    }
}