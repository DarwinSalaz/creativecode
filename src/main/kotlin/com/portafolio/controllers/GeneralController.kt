package com.portafolio.controllers

import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.Company
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.repositories.CompanyRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class GeneralController {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: CompanyRepository

    @Autowired
    private lateinit var userRepository: ApplicationUserRepository

    @GetMapping("/status")
    fun statusController() : ResponseEntity<Map<String, String>>{

        log.info("[statusController] Test for logs in portfolio")

        return ResponseEntity(mapOf("response" to "The microservice 'portafolio_ms' is up"), HttpStatus.OK)
    }

    @GetMapping("/companies/{id_company}")
    fun infoCompany(
        @PathVariable("id_company") idCompany : Int
    ) : Company {

        log.info("[statusController] Test for logs in portfolio")

        return repository.getOne(idCompany)
    }

    @GetMapping("/applicationuser/{username}")
    fun infoUser(
        @PathVariable("username") username: String
    ) : ApplicationUser? {

        return userRepository.findByUsername(username)
    }

}