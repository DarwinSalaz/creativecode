package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.repositories.CashControlRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class CashControlService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: CashControlRepository

    fun save(cashControl: CashControl) : CashControl {

        return repository.save(cashControl)
    }

    @Transactional
    fun updateValueForNewService(cashControl: CashControl, value: BigDecimal) {
        val cash = cashControl.cash.add(value)
        val revenues = cashControl.revenues.add(value)
        val expenses = cashControl.expenses
        val servicesCount = cashControl.servicesCount + 1

        return repository.updateCashControlValues(cash, revenues, expenses, servicesCount, cashControl.cashControlId)
    }

    @Transactional
    fun updateValuesForExpenses(cashControl: CashControl, value: BigDecimal) {
        val cash = cashControl.cash.subtract(value)
        val revenues = cashControl.revenues
        val expenses = cashControl.expenses.add(value)
        val servicesCount = cashControl.servicesCount

        return repository.updateCashControlValues(cash, revenues, expenses, servicesCount, cashControl.cashControlId)
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
                startsDate = LocalDateTime.now(),
                servicesCount = 0
            )

            repository.save(cashControl)
        }

        return cashControl
    }
}