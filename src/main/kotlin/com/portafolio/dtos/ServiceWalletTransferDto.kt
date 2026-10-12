package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

data class ServiceWalletTransferRequest(
    @field:NotNull
    @JsonProperty("service_id")
    val serviceId: Long,

    @field:NotNull
    @JsonProperty("target_wallet_id")
    val targetWalletId: Int,

    @JsonProperty("justification")
    val justification: String? = null
)

data class ServiceWalletTransferPreview(
    @JsonProperty("service_id") val serviceId: Long,
    @JsonProperty("source_wallet_id") val sourceWalletId: Int,
    @JsonProperty("source_wallet_name") val sourceWalletName: String,
    @JsonProperty("target_wallet_id") val targetWalletId: Int,
    @JsonProperty("target_wallet_name") val targetWalletName: String,
    @JsonProperty("cash_control_id") val cashControlId: Long?,
    @JsonProperty("cash_control_closed") val cashControlClosed: Boolean,
    @JsonProperty("payments_count") val paymentsCount: Int,
    @JsonProperty("cash_movements_count") val cashMovementsCount: Int,
    @JsonProperty("customer_id") val customerId: Long,
    @JsonProperty("target_customer_id") val targetCustomerId: Long?,
    @JsonProperty("customer_other_services_count") val customerOtherServicesCount: Long,
    @JsonProperty("customer_strategy") val customerStrategy: String,
    @JsonProperty("customer_strategy_message") val customerStrategyMessage: String,
    @JsonProperty("inventory_wallet_ids") val inventoryWalletIds: List<Int>,
    @JsonProperty("inventory_matches_target_wallet") val inventoryMatchesTargetWallet: Boolean,
    @JsonProperty("inventory_warning") val inventoryWarning: String,
    @JsonProperty("warnings") val warnings: List<String>
)

data class ServiceWalletTransferResponse(
    @JsonProperty("transfer_id") val transferId: Long,
    @JsonProperty("service_id") val serviceId: Long,
    @JsonProperty("source_wallet_id") val sourceWalletId: Int,
    @JsonProperty("target_wallet_id") val targetWalletId: Int,
    @JsonProperty("source_customer_id") val sourceCustomerId: Long,
    @JsonProperty("target_customer_id") val targetCustomerId: Long,
    @JsonProperty("customer_strategy") val customerStrategy: String,
    @JsonProperty("cash_control_id") val cashControlId: Long?,
    @JsonProperty("transferred_at") val transferredAt: LocalDateTime
)
