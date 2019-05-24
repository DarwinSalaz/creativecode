package com.portafolio.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class GeneralController {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/status")
    fun statusController() : ResponseEntity<Map<String, String>>{

        log.info("[statusController] Test for logs in portfolio")

        return ResponseEntity(mapOf("response" to "The microservice 'portafolio_ms' is up"), HttpStatus.OK)
    }

}