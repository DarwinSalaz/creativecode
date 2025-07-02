package com.portafolio.mappers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.dtos.ExpenseResumeDto
import com.portafolio.dtos.ExpenseListResponseDto
import com.portafolio.entities.CashMovement
import com.portafolio.entities.Expense
import com.portafolio.entities.ApplicationUser
import com.portafolio.services.Utilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class ExpenseMapper {

    @Autowired
    lateinit var utilities: Utilities

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun map(expenseDto: ExpenseDto) =
        Expense(
            applicationUserId = expenseDto.applicationUserId,
            expenseType = expenseDto.expenseType,
            value = expenseDto.value,
            expenseDate = expenseDto.expenseDate,
            justification = expenseDto.justification,
            walletId = expenseDto.walletId
        )

    fun mapReverse(cashMovement: CashMovement) =
        ExpenseResumeDto(
            expenseType = cashMovement.description ?: "",
            value = utilities.currencyFormat(cashMovement.value.toPlainString()),
            expenseDate = cashMovement.createdAt.format(formatter),
            justification = cashMovement.justification
        )

    fun mapToListResponse(expense: Expense, username: String) =
        ExpenseListResponseDto(
            expenseId = expense.expenseId,
            expenseType = expense.expenseType,
            value = expense.value,
            expenseDate = expense.expenseDate,
            justification = expense.justification,
            walletId = expense.walletId,
            username = username
        )

}