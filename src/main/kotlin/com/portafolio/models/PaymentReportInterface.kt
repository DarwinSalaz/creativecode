package com.portafolio.models

import java.math.BigDecimal
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