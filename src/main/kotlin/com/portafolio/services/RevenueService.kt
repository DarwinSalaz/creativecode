package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Expense
import com.portafolio.entities.Revenue
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.RevenueRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import javax.transaction.Transactional

@Service
class RevenueService {

    @Autowired
    private lateinit var repository: RevenueRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Transactional
    fun save(revenue: Revenue) : Revenue {
        val activeCashControl : CashControl = cashControlService.findActiveCashControlByUser(revenue.applicationUserId)

        cashControlService.updateValueForInputCash(activeCashControl, revenue.value, BigDecimal.ZERO, BigDecimal.ZERO, false)

        val cashControlId: Long = activeCashControl.cashControlId

        val revenueSaved = repository.save(revenue)

        val cashMovement = CashMovement(
            cashMovementType = "revenue",
            movementType = "IN",
            applicationUserId = revenue.applicationUserId,
            paymentId = null,
            serviceId = null,
            value = revenue.value,
            description = revenue.revenueType,
            cashControlId = cashControlId,
            commission = BigDecimal.ZERO,
            downPayments = BigDecimal.ZERO,
            justification = revenue.justification,
            walletId = revenue.walletId,
            revenueId = revenueSaved.revenueId,
            expenseId = null
        )

        cashMovementRepository.save(cashMovement)

        return revenue
    }

}