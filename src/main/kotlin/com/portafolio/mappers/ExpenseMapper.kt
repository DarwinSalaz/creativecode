package com.portafolio.mappers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.dtos.ExpenseResumeDto
import com.portafolio.entities.Expense
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

    fun mapReverse(expense: Expense) =
        ExpenseResumeDto(
            expenseType = expense.expenseType,
            value = utilities.currencyFormat(expense.value.toPlainString()),
            expenseDate = expense.expenseDate.format(formatter),
            justification = expense.justification
        )

}