package com.portafolio.services

import com.portafolio.dtos.*
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BulkRevenueUploadValidationService {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val MAX_RECORDS = 500
    private val DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val DATE_TIME_FORMAT_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_FORMAT_ISO = DateTimeFormatter.ISO_DATE_TIME

    // Tipos de ingresos válidos
    private val VALID_REVENUE_TYPES = setOf(
        "Efectivo Carteras",
        "Venta Insumos Y Materiales",
        "Abono Deuda",
        "Venta Directa",
        "Liquidación Parcial Cartera",
        "Otros"
    )

    fun validateBulkRevenueUpload(request: BulkRevenueUploadRequest): BulkRevenueValidationResponse {
        log.info("Starting bulk revenue upload validation for ${request.records.size} records with wallet ID ${request.walletId}")

        val errors = mutableListOf<RevenueRecordError>()
        var validRecords = 0
        var totalRevenues = 0
        var estimatedValue = 0.0

        // Validar límite de registros
        if (request.records.size > MAX_RECORDS) {
            errors.add(RevenueRecordError(
                rowNumber = 0,
                field = "records",
                message = "El archivo excede el límite máximo de $MAX_RECORDS registros",
                record = RevenueUploadRecord("", 0.0, "", null, null)
            ))
            return BulkRevenueValidationResponse(
                isValid = false,
                totalRecords = request.records.size,
                validRecords = 0,
                invalidRecords = request.records.size,
                errors = errors,
                summary = RevenueUploadSummary(0, 0.0)
            )
        }

        // Validar cada registro
        for ((index, record) in request.records.withIndex()) {
            val rowNumber = index + 2 // +2 porque empieza en 1 y la primera es header
            val recordErrors = validateRevenueRecord(record, rowNumber)

            if (recordErrors.isEmpty()) {
                validRecords++
                totalRevenues++
                estimatedValue += record.value
            } else {
                errors.addAll(recordErrors)
            }
        }

        val invalidRecords = request.records.size - validRecords

        return BulkRevenueValidationResponse(
            isValid = errors.isEmpty(),
            totalRecords = request.records.size,
            validRecords = validRecords,
            invalidRecords = invalidRecords,
            errors = errors,
            summary = RevenueUploadSummary(totalRevenues, estimatedValue)
        )
    }

    private fun validateRevenueRecord(record: RevenueUploadRecord, rowNumber: Int): List<RevenueRecordError> {
        val errors = mutableListOf<RevenueRecordError>()

        // Validar revenue_type
        if (record.revenueType.isBlank()) {
            errors.add(RevenueRecordError(
                rowNumber = rowNumber,
                field = "revenue_type",
                message = "El tipo de ingreso es obligatorio",
                record = record
            ))
        } else if (!VALID_REVENUE_TYPES.contains(record.revenueType)) {
            errors.add(RevenueRecordError(
                rowNumber = rowNumber,
                field = "revenue_type",
                message = "Tipo de ingreso inválido: '${record.revenueType}'. Tipos válidos: ${VALID_REVENUE_TYPES.joinToString(", ")}",
                record = record
            ))
        }

        // Validar value
        if (record.value <= 0) {
            errors.add(RevenueRecordError(
                rowNumber = rowNumber,
                field = "value",
                message = "El valor debe ser mayor a 0",
                record = record
            ))
        }

        // Validar revenue_date
        try {
            parseRevenueDate(record.revenueDate)
        } catch (e: Exception) {
            errors.add(RevenueRecordError(
                rowNumber = rowNumber,
                field = "revenue_date",
                message = "Formato de fecha inválido: ${record.revenueDate}. Use MM/dd/yyyy o yyyy-MM-dd HH:mm:ss",
                record = record
            ))
        }

        return errors
    }

    fun parseRevenueDate(value: String): LocalDateTime {
        // Intento 1: MM/dd/yyyy
        try {
            return LocalDate.parse(value, DATE_FORMAT_SLASH).atStartOfDay()
        } catch (_: Exception) {}

        // Intento 2: yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMAT_DASH)
        } catch (_: Exception) {}

        // Intento 3: ISO format (yyyy-MM-ddTHH:mm:ss)
        try {
            return LocalDateTime.parse(value, DATE_FORMAT_ISO)
        } catch (_: Exception) {}

        // Intento 4: yyyy-MM-dd
        try {
            return LocalDate.parse(value).atStartOfDay()
        } catch (_: Exception) {}

        // Intento 5: serial numérico de Excel
        try {
            val serial = value.toDouble()
            val epoch = LocalDate.of(1899, 12, 30).atStartOfDay()
            val days = serial.toLong()
            val fraction = serial - days
            val seconds = Math.round(fraction * 24 * 60 * 60).toLong()
            return epoch.plusDays(days).plusSeconds(seconds)
        } catch (_: Exception) {}

        throw IllegalArgumentException("Formato de fecha inválido: $value")
    }
}

