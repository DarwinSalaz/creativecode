package com.portafolio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.builder.SpringApplicationBuilder



@SpringBootApplication
class PortafolioApplication

fun main(args: Array<String>) {
    runApplication<PortafolioApplication>(*args)
}
