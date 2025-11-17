package com.portafolio.controllers

import com.portafolio.dtos.*
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.BulkExpenseUploadService
import com.portafolio.services.BulkExpenseUploadValidationService
import com.portafolio.services.ApplicationUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.DateUtil
import java.text.SimpleDateFormat
import org.springframework.validation.annotation.Validated
import java.io.InputStream
import org.slf4j.LoggerFactory

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class BulkExpenseUploadController {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var validationService: BulkExpenseUploadValidationService

    @Autowired
    lateinit var bulkExpenseUploadService: BulkExpenseUploadService

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @GetMapping("/bulk-expense-upload/template")
    fun downloadTemplate(): ResponseEntity<Any> {
        return try {
            val headers = listOf(
                "expense_type", "value", "expense_date", "justification"
            )

            val exampleRow = listOf(
                "Almuerzo", "50000", "01/15/2024", "Almuerzo de trabajo"
            )

            ResponseEntity.ok(mapOf(
                "message" to "Formato de archivo Excel requerido para gastos",
                "headers" to headers,
                "example" to exampleRow,
                "instructions" to listOf(
                    "El archivo debe tener exactamente 4 columnas",
                    "La primera fila debe contener los encabezados",
                    "Las fechas deben estar en formato MM/dd/yyyy",
                    "Los valores numéricos no deben tener formato de moneda",
                    "Tipos de gasto válidos: Almuerzo, Gasolina, Alquiler, Repuestos moto, Reparacion moto, Materiales E Insumos, Nómina, Contratos, Viáticos, Prestamos, Compra Muebles x Mayor, Impuestos, Otros"
                )
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error generando plantilla: ${e.message}"))
        }
    }

    @PostMapping("/bulk-expense-upload/validate")
    fun validateFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("wallet_id") walletId: Int,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> {
        try {
            val records = parseExcelFile(file.inputStream)
            val request = BulkExpenseUploadRequest(walletId = walletId, records = records)
            val validation = validationService.validateBulkExpenseUpload(request)

            return ResponseEntity.ok(validation)

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Error procesando archivo: ${e.message}"))
        }
    }

    @PostMapping("/bulk-expense-upload/process")
    fun processBulkUpload(
        @RequestBody request: BulkExpenseUploadRequest,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> {
        try {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            val applicationUsername : String = applicationUserService.verifyToken(token)
            val user = applicationUserRepository.findByUsername(applicationUsername)

            user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

            // Validar antes de procesar
            val validation = validationService.validateBulkExpenseUpload(request)
            if (!validation.isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "El archivo contiene errores de validación", "validation" to validation))
            }

            val result = bulkExpenseUploadService.processBulkExpenseUpload(request, applicationUsername)
            return ResponseEntity.ok(result)

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error procesando cargue masivo: ${e.message}"))
        }
    }

    private fun parseExcelFile(inputStream: InputStream): List<ExpenseUploadRecord> {
        val workbook = try {
            WorkbookFactory.create(inputStream)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo Excel: ${e.message}")
        }

        val sheet = workbook.getSheetAt(0)
        val records = mutableListOf<ExpenseUploadRecord>()

        log.info("Excel info: Total sheets: ${workbook.numberOfSheets}, Sheet name: ${sheet.sheetName}")
        log.info("Sheet info: First row: ${sheet.firstRowNum}, Last row: ${sheet.lastRowNum}")

        // Verificar si hay al menos 2 filas (header + data)
        if (sheet.lastRowNum < 1) {
            throw IllegalArgumentException("El archivo Excel debe tener al menos una fila de datos además del header")
        }

        // Verificar el header row
        val headerRow = sheet.getRow(0)
        if (headerRow == null) {
            throw IllegalArgumentException("No se encontró fila de encabezados en el archivo Excel")
        }

        log.info("Header row info: First cell: ${headerRow.firstCellNum}, Last cell: ${headerRow.lastCellNum}, Physical cells: ${headerRow.physicalNumberOfCells}")

        // Verificar que tenga al menos 4 columnas
        if (headerRow.lastCellNum < 4) {
            throw IllegalArgumentException("El archivo Excel debe tener al menos 4 columnas. Encontradas: ${headerRow.lastCellNum}")
        }

        // Saltar la primera fila (headers)
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue

            log.info("Processing row $i: First cell: ${row.firstCellNum}, Last cell: ${row.lastCellNum}, Physical cells: ${row.physicalNumberOfCells}")

            // Verificar si la fila tiene datos reales (al menos alguna celda con contenido)
            val hasData = (0..3).any { colIndex ->
                val cell = row.getCell(colIndex)
                val value = getCellValueAsString(cell)
                value.isNotBlank()
            }

            if (!hasData) {
                log.info("Skipping empty row $i")
                continue
            }

            try {
                // Column order:
                // 0: expense_type
                // 1: value
                // 2: expense_date
                // 3: justification
                val record = ExpenseUploadRecord(
                    expenseType = getCellValueAsString(row.getCell(0)),
                    value = getCellValueAsDouble(row.getCell(1)),
                    expenseDate = getCellValueAsString(row.getCell(2)),
                    justification = getCellValueAsString(row.getCell(3))
                )
                records.add(record)
                log.info("Successfully parsed row $i")
            } catch (e: Exception) {
                log.error("Error parsing row $i: ${e.message}")
                log.error("Exception type: ${e.javaClass.simpleName}")
                log.error("Stack trace:", e)
                // Saltar filas con errores de parsing
                continue
            }
        }

        workbook.close()
        return records
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""

        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue ?: ""
                CellType.NUMERIC -> {
                    // Detectar fechas en celdas numéricas de Excel y convertir a MM/dd/yyyy
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val sdf = SimpleDateFormat("MM/dd/yyyy")
                        return sdf.format(cell.dateCellValue)
                    }
                    // Para números enteros, no mostrar decimales
                    val numValue = cell.numericCellValue
                    if (numValue == numValue.toLong().toDouble()) {
                        numValue.toLong().toString()
                    } else {
                        numValue.toString()
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val sdf = SimpleDateFormat("MM/dd/yyyy")
                            return sdf.format(cell.dateCellValue)
                        }
                        try {
                            cell.stringCellValue ?: ""
                        } catch (e: Exception) {
                            val numValue = cell.numericCellValue
                            if (numValue == numValue.toLong().toDouble()) {
                                numValue.toLong().toString()
                            } else {
                                numValue.toString()
                            }
                        }
                    } catch (e: Exception) {
                        log.warn("Error processing formula cell: ${e.message}")
                        ""
                    }
                }
                CellType.BLANK -> ""
                else -> {
                    log.warn("Unknown cell type: ${cell.cellType}")
                    ""
                }
            }
        } catch (e: Exception) {
            log.warn("Error getting cell value as string: ${e.message}")
            ""
        }
    }

    private fun getCellValueAsDouble(cell: Cell?): Double {
        if (cell == null) return 0.0

        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> {
                    val stringValue = cell.stringCellValue?.trim()
                    if (stringValue.isNullOrEmpty()) {
                        0.0
                    } else {
                        stringValue.toDoubleOrNull() ?: 0.0
                    }
                }
                CellType.FORMULA -> {
                    try {
                        cell.numericCellValue
                    } catch (e: Exception) {
                        try {
                            val stringValue = cell.stringCellValue?.trim()
                            stringValue?.toDoubleOrNull() ?: 0.0
                        } catch (e2: Exception) {
                            log.warn("Error getting formula cell value as double: ${e2.message}")
                            0.0
                        }
                    }
                }
                CellType.BLANK -> 0.0
                else -> {
                    log.warn("Cannot convert cell type ${cell.cellType} to double")
                    0.0
                }
            }
        } catch (e: Exception) {
            log.warn("Error getting cell value as double: ${e.message}")
            0.0
        }
    }
}

