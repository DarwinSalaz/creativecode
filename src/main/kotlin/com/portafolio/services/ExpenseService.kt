package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Expense
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.ExpenseRepository
import com.portafolio.repositories.ApplicationUserRepository
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

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    private lateinit var applicationUserRepository: ApplicationUserRepository

    @Transactional
    fun deleteExpense(expenseId: Long): Boolean {
        val expense = repository.findById(expenseId).orElse(null) ?: return false
        val activeCashControl : CashControl = cashControlService.findActiveCashControlByUser(expense.applicationUserId)
        cashControlService.updateValuesForDeleteExpenses(activeCashControl, expense.value)
        repository.deleteById(expenseId)
        cashMovementRepository.deleteByExpenseId(expenseId)

        return true
    }

    @Transactional
    fun save(expense: Expense) : Expense {
        //validate if the user has an active cash control
        val activeCashControl : CashControl? = cashControlService.findActiveCashControlByUser(expense.applicationUserId)
        val cashControlId: Long

        if(activeCashControl == null) {
            val cashControl = CashControl(
                applicationUserId = expense.applicationUserId,
                active = true,
                cash = expense.value.multiply((-1).toBigDecimal()),
                expenses = expense.value,
                revenues = BigDecimal.ZERO,
                startsDate = LocalDateTime.now(),
                servicesCount = 1,
                commission = BigDecimal.ZERO
            )

            val cashControlSaved = cashControlService.save(cashControl)

            cashControlId = cashControlSaved.cashControlId
        } else {
            cashControlService.updateValuesForExpenses(activeCashControl, expense.value)

            cashControlId = activeCashControl.cashControlId
        }

        val expenseSaved = repository.save(expense)

        val cashMovement = CashMovement(
            cashMovementType = "expense",
            movementType = "OUT",
            applicationUserId = expense.applicationUserId,
            paymentId = null,
            serviceId = null,
            value = expense.value,
            description = expense.expenseType,
            cashControlId = cashControlId,
            commission = BigDecimal.ZERO,
            downPayments = BigDecimal.ZERO,
            justification = expense.justification,
            walletId = expense.walletId,
            expenseId = expenseSaved.expenseId,
            revenueId = null
        )

        cashMovementRepository.save(cashMovement)

        return expense
    }

    fun getExpenses(applicationUserId: Long) = repository.getExpenses(applicationUserId)

    fun getExpensesByControlId(cashControlId: Long) = cashMovementRepository.getExpensesByCashControlId(cashControlId)

    fun getExpensesWithFilters(
        walletId: Int?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        expenseType: String?
    ): List<Expense> {
        return repository.findExpensesWithFilters(walletId, startDate, endDate, expenseType)
    }

    fun getUsernameByUserId(applicationUserId: Long): String {
        val user = applicationUserRepository.findById(applicationUserId).orElse(null)
        return user?.username ?: "unknown"
    }

}