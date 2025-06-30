package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentReportResponse(

    @JsonProperty("total_value")
    var totalValue: String?,

    @JsonProperty("total_debt")
    var totalDebt: String?,

    @JsonProperty("payments_data")
    var paymentsData: List<PaymentReport>

)

data class ExpiredServiceReportResponse(

    @JsonProperty("total_value")
    var totalValue: String?,

    @JsonProperty("expired_services")
    var expiredServices: List<ExpiredServiceReport>

)