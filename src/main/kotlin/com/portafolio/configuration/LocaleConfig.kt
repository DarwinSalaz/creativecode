package com.portafolio.configuration

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.TimeZone
import java.util.Date
import javax.annotation.PostConstruct

@Configuration
class LocaleConfig {

    private val log = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Cordoba"))
        log.info("Date in Argentina: " + Date().toString())
    }
}