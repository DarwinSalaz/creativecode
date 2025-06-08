package com.portafolio.models

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface PaymentReportInterface {

    var id: Long?
    var client: String?
    var service_id: Long?
    var value: BigDecimal?
    var wallet: String?
    var username: String?
    var created_at: LocalDateTime?

}

interface ExpiredServiceReportInterface {
    var client: String?
    var cellphone: String?
    var address: String?
    var total_value: BigDecimal?
    var debt: BigDecimal?
    var pending_fees: Int?
    var next_payment_date: LocalDateTime?
    var created_at: LocalDateTime?
    var last_payment_date: LocalDateTime?
    var expired_fees: Int?
}