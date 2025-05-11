package com.portafolio.controllers

import com.portafolio.dtos.RevenueDto
import com.portafolio.mappers.RevenueMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.RevenueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class RevenueController {

    @Autowired
    lateinit var mapper: RevenueMapper

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var service: RevenueService

    @PostMapping("/revenue/create")
    fun create(@Valid @RequestBody revenueDto : RevenueDto,
               @RequestHeader("Authorization") authorization: String): ResponseEntity<Any> {
        val username: String = if (revenueDto.username.isNullOrEmpty()) {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            applicationUserService.verifyToken(token)
        } else {
            revenueDto.username!!
        }

        val user = applicationUserRepository.findByUsername(username)

        user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

        revenueDto.applicationUserId = user.applicationUserId

        val expenseSaved = service.save(mapper.map(revenueDto))

        return ResponseEntity.status(HttpStatus.OK).body(expenseSaved)
    }

}