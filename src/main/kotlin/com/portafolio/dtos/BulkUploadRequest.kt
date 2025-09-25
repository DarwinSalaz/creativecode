package com.portafolio.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull

data class BulkUploadRequest(
    @field:NotNull(message = "wallet_id is required")
    @JsonProperty("wallet_id")
    val walletId: Int,
    
    @field:NotNull(message = "records is required")
    val records: List<BulkUploadRecord>
)

data class BulkUploadRecord(
    // Cliente fields
    val name: String,
    val last_name: String? = null,
    val cellphone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val identification_number: String? = null,
    val gender: String? = null,
    val observation: String? = null,
    
    // Servicio fields
    val valor_servicio: Double,
    val cuota_inicial: Double,
    val descuento: Double,
    val deuda: Double,
    val valor_total: Double,
    val dias_cuota: Int,
    val nro_cuotas: Int,
    val valor_cuota: Double,
    val abono: Double? = 0.0,
    val next_payment_date: String,
    val application_user_id: Long? = null,
    
    // Productos
    val product_name: String,
    val product_quantity: String
)

data class BulkUploadValidationResponse(
    val isValid: Boolean,
    val totalRecords: Int,
    val validRecords: Int,
    val invalidRecords: Int,
    val errors: List<RecordError>,
    val summary: UploadSummary
)

data class RecordError(
    val rowNumber: Int,
    val field: String,
    val message: String,
    val record: BulkUploadRecord
)

data class UploadSummary(
    val totalCustomers: Int,
    val totalServices: Int,
    val totalProducts: Int,
    val estimatedValue: Double
)