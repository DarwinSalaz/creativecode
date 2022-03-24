package com.portafolio.services

import com.portafolio.dtos.CashControlClosureRequest
import com.portafolio.dtos.CashControlResponse
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.CashControl
import com.portafolio.repositories.CashControlRepository
import com.portafolio.repositories.CashMovementRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
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
    lateinit var utilities: Utilities

    fun save(cashControl: CashControl) : CashControl {

        return repository.save(cashControl)
    }

    @Transactional
    fun updateValueForInputCash(cashControl: CashControl, value: BigDecimal, isNewService: Boolean = false) {
        val cash = cashControl.cash.add(value)
        val revenues = cashControl.revenues.add(value)
        val expenses = cashControl.expenses
        val servicesCount = cashControl.servicesCount + 1
        val commission = if (isNewService) cashControl.commission?.add(value) ?: BigDecimal.ZERO else cashControl.commission?.add(value.multiply(0.12.toBigDecimal())) ?: value.multiply(0.12.toBigDecimal())
        val downPayments = if (isNewService) cashControl.downPayments?.add(value) ?: value else BigDecimal.ZERO

        return repository.updateCashControlValues(cash, revenues, expenses, commission, servicesCount, downPayments, cashControl.cashControlId)
    }

    @Transactional
    fun updateValuesForExpenses(cashControl: CashControl, value: BigDecimal) {
        val cash = cashControl.cash.subtract(value)
        val revenues = cashControl.revenues
        val expenses = cashControl.expenses.add(value)
        val servicesCount = cashControl.servicesCount
        val downPayments = cashControl.downPayments ?: BigDecimal.ZERO
        val commissions = cashControl.commission ?: BigDecimal.ZERO

        return repository.updateCashControlValues(cash, revenues, expenses, commissions, servicesCount, downPayments, cashControl.cashControlId)
    }

    fun findActiveCashControlByUser(applicationUserId : Long) : CashControl? {

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
        cashControl.commission = request.commission
        cashControl.closureDate = LocalDateTime.now()
        cashControl.closureUser = userClosure
        cashControl.closureValueReceived = request.closureValueReceived
        cashControl.closureNotes = request.closureNotes

        return repository.save(cashControl)
    }

    fun getDailyCashControl(applicationUser : ApplicationUser) : CashControlResponse {
        val cashControl = findActiveCashControlByUser(applicationUser.applicationUserId)

        var movements = cashMovementRepository.findCashMovementsByCashControlId(cashControl!!.cashControlId)

        movements = movements
            .filter {
                it.createdAt.truncatedTo(ChronoUnit.DAYS) == LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
            }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startsDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val endsDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
        val period = startsDate.toLocalDate().format(formatter) + "/" + endsDate.toLocalDate().format(formatter)
        val revenues = movements.filter { it.movementType == "IN" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val expenses = movements.filter { it.movementType == "OUT" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val commissions = movements.filter { it.movementType == "IN" }.mapNotNull { it.commission }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }
        val downPayments = movements.filter { it.movementType == "IN" && it.cashMovementType == "new_service" }.map { it.value }.fold (BigDecimal.ZERO) { a, b -> a.add(b) }

        return CashControlResponse (
            fullName = if (applicationUser.lastName == null) applicationUser.name else applicationUser.name + " " + applicationUser.lastName,
            cashControlId = cashControl.cashControlId,
            applicationUserId = applicationUser.applicationUserId,
            startsDate = startsDate,
            endsDate = endsDate,
            revenues = utilities.currencyFormat(revenues.toString()),
            expenses = utilities.currencyFormat(expenses.toString()),
            cash = utilities.currencyFormat(revenues.subtract(expenses).subtract(commissions).toString()),
            active = cashControl.active,
            period = period,
            servicesCount = movements.filter { it.movementType == "IN" }.size,
            cashNumber = revenues.subtract(expenses),
            commission = utilities.currencyFormat(commissions.toString()),
            commissionNumber = commissions ?: BigDecimal.ZERO,
            downPayments = utilities.currencyFormat(downPayments.toString()),
            downPaymentsNumber = downPayments ?: BigDecimal.ZERO
        )
    }
}