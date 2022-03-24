package com.portafolio.mappers

import com.portafolio.dtos.CashControlResponse
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.CashControl
import com.portafolio.services.Utilities
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class CashControlMapper {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var utilities: Utilities

    fun map(cashControl: CashControl?, applicationUser: ApplicationUser) : CashControlResponse? {

        if (cashControl == null) return null

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startsDate = cashControl.startsDate.toLocalDate().format(formatter)
        val endsDate = cashControl.endsDate?.toLocalDate()?.format(formatter)
        val period = startsDate + "/" + (endsDate ?: "Actual")

        return CashControlResponse(
            fullName = if (applicationUser.lastName == null) applicationUser.name else applicationUser.name + " " + applicationUser.lastName,
            cashControlId = cashControl.cashControlId,
            startsDate = cashControl.startsDate,
            revenues = utilities.currencyFormat(cashControl.revenues.toString()),
            expenses = utilities.currencyFormat(cashControl.expenses.toString()),
            cash = utilities.currencyFormat(cashControl.cash.subtract(cashControl.expenses).subtract(cashControl.commission).toString()),
            applicationUserId = cashControl.applicationUserId,
            active = cashControl.active,
            period = period,
            servicesCount = cashControl.servicesCount,
            cashNumber = cashControl.cash,
            commission = utilities.currencyFormat(cashControl.commission?.toString() ?: "$0"),
            commissionNumber = cashControl.commission ?: BigDecimal.ZERO,
            downPayments = utilities.currencyFormat(cashControl.downPayments?.toString() ?: "$0"),
            downPaymentsNumber = cashControl.downPayments ?: BigDecimal.ZERO
        )

    }


}