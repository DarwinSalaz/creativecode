package com.portafolio.services

import com.portafolio.dtos.ResumeWallet
import com.portafolio.entities.CashMovement
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class CashMovementService {

    @Autowired
    lateinit var cashMovementRepository: CashMovementRepository

    @Autowired
    lateinit var walletRepository: WalletRepository

    @Autowired
    lateinit var utilities: Utilities

    fun getCashMovementsByWallet(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : ResumeWallet {
        val movements = cashMovementRepository.getCashMovementsByWallet(walletId, startsAt.truncatedTo(ChronoUnit.DAYS), endsAt.withHour(23).withMinute(59).withSecond(59))

        val servicesCount = movements.filter { it.cashMovementType == "new_service" }.size
        val inputs = movements.filter { it.movementType == "IN" }
        val outputs = movements.filter { it.movementType == "OUT" }
        val cash = inputs.map { it.value }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val downPayments = inputs.map { it.downPayments }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val commissions = inputs.map { it.commission }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}
        val expenses = outputs.map { it.value }.fold(BigDecimal.ZERO) {a, b -> a.add(b)}

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
}