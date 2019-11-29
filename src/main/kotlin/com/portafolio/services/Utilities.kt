package com.portafolio.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@Component
class Utilities {

    private val log = LoggerFactory.getLogger(this::class.java)

    val locale = Locale("es", "AR")

    public fun currencyFormat(strNum: String): String {
        var currencyFormat: String
        try {
            val numberFormat = NumberFormat.getCurrencyInstance(locale)
            numberFormat.minimumFractionDigits = 0
            currencyFormat = numberFormat.format(BigDecimal(strNum))
        } catch (e: Exception) {
            log.error("It is not possible to convert to currency format, value: {}, locale: {}", strNum, locale.country,e)
            currencyFormat = strNum
        }

        return currencyFormat
    }

}