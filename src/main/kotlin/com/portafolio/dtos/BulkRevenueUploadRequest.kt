package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull

data class BulkRevenueUploadRequest(
    @field:NotNull(message = "wallet_id is required")
    @JsonProperty("wallet_id")
    val walletId: Int,
    
    @field:NotNull(message = "records is required")
    val records: List<RevenueUploadRecord>
)

data class RevenueUploadRecord(
    @JsonProperty("revenue_type")
    val revenueType: String,
    
    @JsonProperty("value")
    val value: Double,
    
    @JsonProperty("revenue_date")
    val revenueDate: String,
    
    @JsonProperty("justification")
    val justification: String? = null,
    
    @JsonProperty("username")
    val username: String? = null
)

data class BulkRevenueValidationResponse(
    val isValid: Boolean,
    val totalRecords: Int,
    val validRecords: Int,
    val invalidRecords: Int,
    val errors: List<RevenueRecordError>,
    val summary: RevenueUploadSummary
)

data class RevenueRecordError(
    val rowNumber: Int,
    val field: String,
    val message: String,
    val record: RevenueUploadRecord
)

data class RevenueUploadSummary(
    val totalRevenues: Int,
    val estimatedValue: Double
)

data class BulkRevenueUploadResult(
    val totalProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<RevenueRecordResult>
)

data class RevenueRecordResult(
    val rowNumber: Int,
    val success: Boolean,
    val revenueId: Long?,
    val message: String
)

