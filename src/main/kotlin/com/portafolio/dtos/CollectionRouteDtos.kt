package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CollectionRouteCreateRequest(
    @JsonProperty("wallet_id") val walletId: Int,
    @JsonProperty("route_date") val routeDate: LocalDate,
    val name: String? = null,
    @JsonProperty("service_ids") val serviceIds: List<Long> = emptyList()
)

data class CollectionRouteStatusRequest(val status: String)
data class CollectionRouteStopStatusRequest(val status: String, val observation: String? = null)
data class CollectionRouteReorderRequest(@JsonProperty("stop_ids") val stopIds: List<Long>)

data class CollectionRouteStopResponse(
    @JsonProperty("route_stop_id") val routeStopId: Long,
    @JsonProperty("service_id") val serviceId: Long,
    @JsonProperty("customer_id") val customerId: Long,
    @JsonProperty("customer_name") val customerName: String,
    val cellphone: String?,
    val address: String?,
    @JsonProperty("next_payment_date") val nextPaymentDate: LocalDateTime?,
    @JsonProperty("fee_value") val feeValue: BigDecimal,
    val debt: BigDecimal,
    val position: Int,
    val status: String,
    val observation: String?
)

data class CollectionRouteResponse(
    @JsonProperty("route_id") val routeId: Long,
    @JsonProperty("wallet_id") val walletId: Int,
    @JsonProperty("wallet_name") val walletName: String,
    @JsonProperty("route_date") val routeDate: LocalDate,
    val name: String,
    val status: String,
    @JsonProperty("created_at") val createdAt: LocalDateTime,
    val stops: List<CollectionRouteStopResponse> = emptyList()
)

data class CollectionRouteSuggestionResponse(
    @JsonProperty("service_id") val serviceId: Long,
    @JsonProperty("customer_id") val customerId: Long,
    @JsonProperty("customer_name") val customerName: String,
    val cellphone: String?,
    val address: String?,
    @JsonProperty("next_payment_date") val nextPaymentDate: LocalDateTime?,
    @JsonProperty("fee_value") val feeValue: BigDecimal,
    val debt: BigDecimal,
    val overdue: Boolean
)
