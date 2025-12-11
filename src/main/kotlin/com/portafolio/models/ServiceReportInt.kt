package com.portafolio.models

import java.math.BigDecimal
import java.time.LocalDateTime

interface ServiceReportInt {

    var id: Long?
    var client: String?
    var products: String?
    var product_values: BigDecimal?
    var discount: BigDecimal?
    var service_value: BigDecimal?
    var debt: BigDecimal?
    var quantity_of_fees: Int?
    var pending_fees: Int?
    var wallet: String?
    var username: String?
    var created_at: LocalDateTime?
}