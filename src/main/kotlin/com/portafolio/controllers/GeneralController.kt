package com.portafolio.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class GeneralController {

    @GetMapping("/status")
    fun statusController() = ResponseEntity(mapOf("response" to "The microservice is up"), HttpStatus.OK)

}