package com.portafolio.models

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentReportResponse(

    @JsonProperty("total_value")
    var totalValue: String?,

    @JsonProperty("payments_data")
    var paymentsData: List<PaymentReport>

)
