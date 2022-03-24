package com.portafolio.controllers

import com.portafolio.dtos.CashControlClosureRequest
import com.portafolio.dtos.CashControlResponse
import com.portafolio.mappers.CashControlMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.CashControlService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
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
    fun getActiveCashControl(@RequestHeader("Authorization", required = false) authorization: String?,
        @RequestParam("username", required = false) usernameParam: String?): CashControlResponse? {

        val applicationUsername = if (usernameParam == null) {
            val token = if (authorization!!.contains("Bearer")) authorization.split(" ")[1] else authorization
            applicationUserService.verifyToken(token)
        } else {
            usernameParam
        }
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        return cashControlMapper.map(service.findActiveCashControlByUser(user.applicationUserId), user)
    }

    @GetMapping("/cash_control/daily")
    fun getDailyCashControl(@RequestHeader("Authorization", required = false) authorization: String?,
                             @RequestParam("username", required = false) usernameParam: String?): CashControlResponse? {

        val applicationUsername = if (usernameParam == null) {
            val token = if (authorization!!.contains("Bearer")) authorization.split(" ")[1] else authorization
            applicationUserService.verifyToken(token)
        } else {
            usernameParam
        }
        val user = applicationUserRepository.findByUsername(applicationUsername)

        user ?: return null

        return service.getDailyCashControl(user)
    }

    @PutMapping("/cash_control/closure")
    fun closureAccount(
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody request: CashControlClosureRequest
    )  : ResponseEntity<HashMap<String, String>> {
        val username: String
        try {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            username = applicationUserService.verifyToken(token)
        } catch (e: Exception) {
            return ResponseEntity.status(401).body(hashMapOf("ok" to "false", "error" to "Error al autenticar al usuario, e: ${e.message}"))
        }

        val cashControl = service.closureAccount(request, username)

        cashControl ?: return ResponseEntity.badRequest().body(hashMapOf("ok" to "false"))

        return ResponseEntity.ok().body(hashMapOf("ok" to "true"))
    }

}