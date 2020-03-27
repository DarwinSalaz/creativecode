package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.Expense
import com.portafolio.entities.Payment
import com.portafolio.repositories.ExpenseRepository
import com.portafolio.repositories.PaymentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class ExpenseService {

    @Autowired
    private lateinit var repository: ExpenseRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Transactional
    fun save(expense: Expense) : Expense {
        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(expense.applicationUserId)

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = expense.applicationUserId,
                active = true,
                cash = expense.value,
                expenses = expense.value,
                revenues = BigDecimal.ZERO,
                startsDate = LocalDateTime.now(),
                servicesCount = 1
            )

            cashControlService.save(cashControl)
        } else {
            cashControlService.updateValuesForExpenses(activeCashControl, expense.value)
        }

        return repository.save(expense)
    }
}