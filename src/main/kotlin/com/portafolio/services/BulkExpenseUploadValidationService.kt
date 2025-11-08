package com.portafolio.services

import com.portafolio.dtos.*
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class BulkExpenseUploadValidationService {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val MAX_RECORDS = 500
    private val DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val DATE_TIME_FORMAT_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_FORMAT_ISO = DateTimeFormatter.ISO_DATE_TIME

    // Tipos de gastos válidos
    private val VALID_EXPENSE_TYPES = setOf(
        "Almuerzo",
        "Gasolina",
        "Alquiler",
        "Repuestos moto",
        "Reparacion moto",
        "Materiales E Insumos",
        "Nómina",
        "Contratos",
        "Viáticos",
        "Prestamos",
        "Compra Muebles x Mayor",
        "Impuestos",
        "Otros"
    )

    fun validateBulkExpenseUpload(request: BulkExpenseUploadRequest): BulkExpenseValidationResponse {
        log.info("Starting bulk expense upload validation for ${request.records.size} records with wallet ID ${request.walletId}")

        val errors = mutableListOf<ExpenseRecordError>()
        var validRecords = 0
        var totalExpenses = 0
        var estimatedValue = 0.0

        // Validar límite de registros
        if (request.records.size > MAX_RECORDS) {
            errors.add(ExpenseRecordError(
                rowNumber = 0,
                field = "records",
                message = "El archivo excede el límite máximo de $MAX_RECORDS registros",
                record = ExpenseUploadRecord("", 0.0, "", null, null)
            ))
            return BulkExpenseValidationResponse(
                isValid = false,
                totalRecords = request.records.size,
                validRecords = 0,
                invalidRecords = request.records.size,
                errors = errors,
                summary = ExpenseUploadSummary(0, 0.0)
            )
        }

        // Validar cada registro
        for ((index, record) in request.records.withIndex()) {
            val rowNumber = index + 2 // +2 porque empieza en 1 y la primera es header
            val recordErrors = validateExpenseRecord(record, rowNumber)

            if (recordErrors.isEmpty()) {
                validRecords++
                totalExpenses++
                estimatedValue += record.value
            } else {
                errors.addAll(recordErrors)
            }
        }

        val invalidRecords = request.records.size - validRecords
        val isValid = errors.isEmpty()

        log.info("Validation completed: totalRecords=${request.records.size}, validRecords=$validRecords, invalidRecords=$invalidRecords, errors.size=${errors.size}, isValid=$isValid")

        return BulkExpenseValidationResponse(
            isValid = isValid,
            totalRecords = request.records.size,
            validRecords = validRecords,
            invalidRecords = invalidRecords,
            errors = errors,
            summary = ExpenseUploadSummary(totalExpenses, estimatedValue)
        )
    }

    private fun validateExpenseRecord(record: ExpenseUploadRecord, rowNumber: Int): List<ExpenseRecordError> {
        val errors = mutableListOf<ExpenseRecordError>()

        // Validar expense_type
        if (record.expenseType.isBlank()) {
            errors.add(ExpenseRecordError(
                rowNumber = rowNumber,
                field = "expense_type",
                message = "El tipo de gasto es obligatorio",
                record = record
            ))
        } else if (!VALID_EXPENSE_TYPES.contains(record.expenseType)) {
            errors.add(ExpenseRecordError(
                rowNumber = rowNumber,
                field = "expense_type",
                message = "Tipo de gasto inválido: '${record.expenseType}'. Tipos válidos: ${VALID_EXPENSE_TYPES.joinToString(", ")}",
                record = record
            ))
        }

        // Validar value
        if (record.value <= 0) {
            errors.add(ExpenseRecordError(
                rowNumber = rowNumber,
                field = "value",
                message = "El valor debe ser mayor a 0",
                record = record
            ))
        }

        // Validar expense_date
        try {
            parseExpenseDate(record.expenseDate)
        } catch (e: Exception) {
            errors.add(ExpenseRecordError(
                rowNumber = rowNumber,
                field = "expense_date",
                message = "Formato de fecha inválido: ${record.expenseDate}. Use MM/dd/yyyy o yyyy-MM-dd HH:mm:ss",
                record = record
            ))
        }

        return errors
    }

    fun parseExpenseDate(value: String): LocalDateTime {
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

