package com.portafolio.mappers

import com.portafolio.dtos.CashControlResponse
import com.portafolio.dtos.CashMovementDto
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.services.Utilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Component
class CashControlMapper {

    @Autowired
    lateinit var utilities: Utilities

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun map(cashControl: CashControl?, applicationUser: ApplicationUser) : CashControlResponse? {

        if (cashControl == null) return null

        val startsDate = cashControl.startsDate.toLocalDate().format(formatter)
        val endsDate = cashControl.endsDate?.toLocalDate()?.format(formatter)
        val period = startsDate + "/" + (endsDate ?: "Actual")

        return CashControlResponse(
            fullName = if (applicationUser.lastName == null) applicationUser.name else applicationUser.name + " " + applicationUser.lastName,
            cashControlId = cashControl.cashControlId,
            startsDate = cashControl.startsDate,
            revenues = utilities.currencyFormat(cashControl.revenues.toString()),
            expenses = utilities.currencyFormat(cashControl.expenses.toString()),
            cash = utilities.currencyFormat(cashControl.cash.toString()),
            applicationUserId = cashControl.applicationUserId,
            active = cashControl.active,
            period = period,
            servicesCount = cashControl.servicesCount,
            cashNumber = cashControl.cash,
            commission = utilities.currencyFormat(cashControl.commission.toString()),
            commissionNumber = cashControl.commission,
            downPayments = utilities.currencyFormat(cashControl.downPayments?.toString() ?: "0"),
            downPaymentsNumber = cashControl.downPayments ?: BigDecimal.ZERO
        )

    }

    fun mapCashMovements(cashMovements: List<CashMovement>) =
        cashMovements.map {
            val createdAt = it.createdAt.toLocalDate().format(formatter)

            CashMovementDto(
                cashMovementType = it.cashMovementType,
                movementType = it.movementType,
                paymentId = it.paymentId,
                serviceId = it.serviceId,
                value = utilities.currencyFormat(it.value.add(it.commission).add(it.downPayments ?: BigDecimal.ZERO).toPlainString()),
                commission = utilities.currencyFormat(it.commission.toPlainString()),
                downPayments = utilities.currencyFormat(it.downPayments?.toPlainString() ?: BigDecimal.ZERO.toPlainString()),
                createdAt = createdAt,
                description = it.description
            )
        }


}