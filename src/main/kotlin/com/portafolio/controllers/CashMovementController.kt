package com.portafolio.controllers

import com.portafolio.dtos.ResumeWalletRequest
import com.portafolio.dtos.UserMovementsReportRequest
import com.portafolio.dtos.UserMovementsReportResponse
import com.portafolio.services.CashMovementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class CashMovementController {

    @Autowired
    lateinit var cashMovementService: CashMovementService

    @PostMapping("/cash_movement/wallet_resume")
    fun getCashMovementsByWallet(@Valid @RequestBody request: ResumeWalletRequest)
        = cashMovementService.getCashMovementsByWallet(request.walletId, request.startsAt, request.endsAt)

    @PostMapping("/cash_movement/user_report")
    fun getUserMovementsReport(@Valid @RequestBody request: UserMovementsReportRequest): UserMovementsReportResponse {
        return cashMovementService.getUserMovementsReport(
            request.applicationUserId, 
            request.startsAt, 
            request.endsAt
        )
    }

}
