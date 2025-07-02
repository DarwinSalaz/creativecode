package com.portafolio.services

import com.portafolio.entities.CashControl
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Expense
import com.portafolio.entities.Revenue
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.RevenueRepository
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class RevenueService {

    @Autowired
    private lateinit var repository: RevenueRepository

    @Autowired
    private lateinit var cashControlService: CashControlService

    @Autowired
    private lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    private lateinit var applicationUserRepository: ApplicationUserRepository

    @Transactional
    fun deleteRevenue(revenueId: Long): Boolean {
        val revenue = repository.findById(revenueId).orElse(null) ?: return false
        val activeCashControl : CashControl = cashControlService.findActiveCashControlByUser(revenue.applicationUserId)
        cashControlService.updateValuesForDeleteRevenues(activeCashControl, revenue.value)
        repository.deleteById(revenueId)
        cashMovementRepository.deleteByRevenueId(revenueId)

        return true
    }

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

    fun getRevenues(applicationUserId: Long) = repository.getRevenues(applicationUserId)

    fun getRevenuesWithFilters(
        walletId: Int?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        revenueType: String?
    ): List<Revenue> {
        return when {
            // Todos los filtros están presentes
            walletId != null && startDate != null && endDate != null && revenueType != null -> {
                repository.findByWalletIdAndRevenueTypeAndDateRange(walletId, revenueType, startDate, endDate)
            }
            // Solo wallet y fechas
            walletId != null && startDate != null && endDate != null -> {
                repository.findByWalletIdAndDateRange(walletId, startDate, endDate)
            }
            // Solo tipo de ingreso y fechas
            revenueType != null && startDate != null && endDate != null -> {
                repository.findByRevenueTypeAndDateRange(revenueType, startDate, endDate)
            }
            // Solo fechas
            startDate != null && endDate != null -> {
                repository.findByDateRange(startDate, endDate)
            }
            // Solo wallet y tipo de ingreso
            walletId != null && revenueType != null -> {
                repository.findByWalletIdAndRevenueType(walletId, revenueType)
            }
            // Solo wallet
            walletId != null -> {
                repository.findByWalletId(walletId)
            }
            // Solo tipo de ingreso
            revenueType != null -> {
                repository.findByRevenueType(revenueType)
            }
            // Sin filtros - retornar todos
            else -> {
                repository.findAll().sortedByDescending { it.revenueDate }
            }
        }
    }

    fun getUsernameByUserId(applicationUserId: Long): String {
        val user = applicationUserRepository.findById(applicationUserId).orElse(null)
        return user?.username ?: "unknown"
    }

}