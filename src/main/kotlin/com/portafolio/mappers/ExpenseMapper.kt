package com.portafolio.mappers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.entities.Expense
import org.springframework.stereotype.Component

@Component
class ExpenseMapper {

    fun map(expenseDto: ExpenseDto) =
        Expense(
            applicationUserId = expenseDto.applicationUserId,
            expenseType = expenseDto.expenseType,
            value = expenseDto.value,
            expenseDate = expenseDto.expenseDate,
            justification = expenseDto.justification,
            walletId = expenseDto.walletId
        )

}