package com.portafolio.mappers

import com.portafolio.dtos.ExpenseDto
import com.portafolio.dtos.RevenueDto
import com.portafolio.dtos.RevenueListResponseDto
import com.portafolio.entities.Expense
import com.portafolio.entities.Revenue
import com.portafolio.services.Utilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class RevenueMapper {

    @Autowired
    lateinit var utilities: Utilities

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun map(revenueDto: RevenueDto) =
        Revenue(
            applicationUserId = revenueDto.applicationUserId,
            revenueType = revenueDto.revenueType,
            value = revenueDto.value,
            revenueDate = revenueDto.revenueDate,
            justification = revenueDto.justification,
            walletId = revenueDto.walletId
        )

    fun mapToListResponse(revenue: Revenue, username: String) =
        RevenueListResponseDto(
            revenueId = revenue.revenueId,
            revenueType = revenue.revenueType,
            value = revenue.value,
            revenueDate = revenue.revenueDate,
            justification = revenue.justification,
            walletId = revenue.walletId,
            username = username
        )

}