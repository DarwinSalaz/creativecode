package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull

data class BulkExpenseUploadRequest(
    @field:NotNull(message = "wallet_id is required")
    @JsonProperty("wallet_id")
    val walletId: Int,
    
    @field:NotNull(message = "records is required")
    val records: List<ExpenseUploadRecord>
)

data class ExpenseUploadRecord(
    @JsonProperty("expense_type")
    val expenseType: String,
    
    @JsonProperty("value")
    val value: Double,
    
    @JsonProperty("expense_date")
    val expenseDate: String,
    
    @JsonProperty("justification")
    val justification: String? = null,
    
    @JsonProperty("username")
    val username: String? = null
)

data class BulkExpenseValidationResponse(
    @JsonProperty("isValid")
    val isValid: Boolean,
    @JsonProperty("totalRecords")
    val totalRecords: Int,
    @JsonProperty("validRecords")
    val validRecords: Int,
    @JsonProperty("invalidRecords")
    val invalidRecords: Int,
    @JsonProperty("errors")
    val errors: List<ExpenseRecordError>,
    @JsonProperty("summary")
    val summary: ExpenseUploadSummary
)

data class ExpenseRecordError(
    val rowNumber: Int,
    val field: String,
    val message: String,
    val record: ExpenseUploadRecord
)

data class ExpenseUploadSummary(
    val totalExpenses: Int,
    val estimatedValue: Double
)

data class BulkExpenseUploadResult(
    val totalProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<ExpenseRecordResult>
)

data class ExpenseRecordResult(
    val rowNumber: Int,
    val success: Boolean,
    val expenseId: Long?,
    val message: String
)

