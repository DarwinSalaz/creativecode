package com.portafolio.controllers

import com.portafolio.dtos.CashControlResponse
import com.portafolio.entities.CashControl
import com.portafolio.mappers.CashControlMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.CashControlService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class CashControlController {

    @Autowired
    private lateinit var service: CashControlService

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var cashControlMapper: CashControlMapper

    @GetMapping("/cash_control/active")
    fun getActiveCashControl(@RequestHeader("Authorization") authorization: String): CashControlResponse? {

        val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
        val applicationUsername : String = applicationUserService.verifyToken(token)
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        return cashControlMapper.map(service.findActiveCashControlByUser(user.applicationUserId), user)
    }


}