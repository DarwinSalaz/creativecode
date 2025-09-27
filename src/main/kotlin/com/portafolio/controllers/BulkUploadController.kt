package com.portafolio.controllers

import com.portafolio.dtos.*
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.BulkUploadService
import com.portafolio.services.BulkUploadValidationService
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
class BulkUploadController {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var validationService: BulkUploadValidationService

    @Autowired
    lateinit var bulkUploadService: BulkUploadService

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @GetMapping("/bulk-upload/template")
    fun downloadTemplate(): ResponseEntity<Any> {
        return try {
            val headers = listOf(
                "codigo", "name", "last_name", "cellphone", "email", "address",
                "identification_number", "gender", "observation", "product_name",
                "product_quantity", "valor_servicio", "descuento", "valor_total",
                "cuota_inicial", "abono", "deuda", "nro_cuotas", "dias_cuota",
                "valor_cuota", "next_payment_date"
            )

            val exampleRow = listOf(
                "001", "Juan", "Pérez", "3001234567", "juan@email.com", "Calle 123",
                "12345678", "m", "Cliente ejemplo", "Producto A|Producto B",
                "1|2", "100000", "5000", "95000", "20000", "10000", "65000",
                "3", "30", "21666.67", "01/15/2024"
            )

            ResponseEntity.ok(mapOf(
                "message" to "Formato de archivo Excel requerido",
                "headers" to headers,
                "example" to exampleRow,
                "instructions" to listOf(
                    "El archivo debe tener exactamente 21 columnas",
                    "La primera fila debe contener los encabezados",
                    "Los productos múltiples se separan con |",
                    "Las cantidades deben corresponder al mismo orden de productos",
                    "Las fechas deben estar en formato MM/dd/yyyy",
                    "Los valores numéricos no deben tener formato de moneda"
                )
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error generando plantilla: ${e.message}"))
        }
    }

    @PostMapping("/bulk-upload/validate")
    fun validateFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("wallet_id") walletId: Int,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> {
        try {
            //val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            //val username = applicationUserService.verifyToken(token)

            val records = parseExcelFile(file.inputStream)
            val request = BulkUploadRequest(walletId = walletId, records = records)
            val validation = validationService.validateBulkUpload(request)

            return ResponseEntity.ok(validation)

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Error procesando archivo: ${e.message}"))
        }
    }

    @PostMapping("/bulk-upload/process")
    fun processBulkUpload(
        @RequestBody request: BulkUploadRequest,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> {
        try {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            val applicationUsername : String = applicationUserService.verifyToken(token)
            val user = applicationUserRepository.findByUsername(applicationUsername)

            user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED")

            // Validar antes de procesar
            val validation = validationService.validateBulkUpload(request)
            if (!validation.isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "El archivo contiene errores de validación", "validation" to validation))
            }

            val result = bulkUploadService.processBulkUpload(request, authorization)
            return ResponseEntity.ok(result)

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error procesando cargue masivo: ${e.message}"))
        }
    }

    private fun parseExcelFile(inputStream: InputStream): List<BulkUploadRecord> {
        val workbook = try {
            WorkbookFactory.create(inputStream)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo Excel: ${e.message}")
        }

        val sheet = workbook.getSheetAt(0)
        val records = mutableListOf<BulkUploadRecord>()

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

        // Verificar que tenga al menos 21 columnas
        if (headerRow.lastCellNum < 21) {
            throw IllegalArgumentException("El archivo Excel debe tener al menos 21 columnas. Encontradas: ${headerRow.lastCellNum}")
        }

        // Saltar la primera fila (headers)
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue

            log.info("Processing row $i: First cell: ${row.firstCellNum}, Last cell: ${row.lastCellNum}, Physical cells: ${row.physicalNumberOfCells}")

            try {
                // Column order provided by user (index -> field):
                // 0: codigo (ignored)
                // 1: name
                // 2: last_name
                // 3: cellphone
                // 4: email
                // 5: address
                // 6: identification_number
                // 7: gender
                // 8: observation
                // 9: product_name
                // 10: product_quantity
                // 11: valor_servicio
                // 12: descuento
                // 13: valor_total
                // 14: cuota_inicial
                // 15: abono
                // 16: deuda
                // 17: nro_cuotas
                // 18: dias_cuota
                // 19: valor_cuota
                // 20: next_payment_date
                val record = BulkUploadRecord(
                    name = getCellValueAsString(row.getCell(1)),
                    last_name = getCellValueAsString(row.getCell(2)),
                    cellphone = getCellValueAsString(row.getCell(3)),
                    email = getCellValueAsString(row.getCell(4)),
                    address = getCellValueAsString(row.getCell(5)),
                    identification_number = getCellValueAsString(row.getCell(6)),
                    gender = getCellValueAsString(row.getCell(7)),
                    observation = getCellValueAsString(row.getCell(8)),
                    product_name = getCellValueAsString(row.getCell(9)),
                    product_quantity = getCellValueAsString(row.getCell(10)),
                    valor_servicio = getCellValueAsDouble(row.getCell(11)),
                    descuento = getCellValueAsDouble(row.getCell(12)),
                    valor_total = getCellValueAsDouble(row.getCell(13)),
                    cuota_inicial = getCellValueAsDouble(row.getCell(14)),
                    abono = getCellValueAsDouble(row.getCell(15)),
                    deuda = getCellValueAsDouble(row.getCell(16)),
                    nro_cuotas = getCellValueAsInt(row.getCell(17)),
                    dias_cuota = getCellValueAsInt(row.getCell(18)),
                    valor_cuota = getCellValueAsDouble(row.getCell(19)),
                    next_payment_date = getCellValueAsString(row.getCell(20))
                    // application_user_id omitted (not present in file)
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
                        // Si la celda de fórmula es fecha, formatear como fecha
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val sdf = SimpleDateFormat("MM/dd/yyyy")
                            return sdf.format(cell.dateCellValue)
                        }
                        // Intentar obtener el valor como string primero
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

    private fun getCellValueAsInt(cell: Cell?): Int {
        if (cell == null) return 0

        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toInt()
                CellType.STRING -> {
                    val stringValue = cell.stringCellValue?.trim()
                    if (stringValue.isNullOrEmpty()) {
                        0
                    } else {
                        stringValue.toIntOrNull() ?: 0
                    }
                }
                CellType.FORMULA -> {
                    try {
                        cell.numericCellValue.toInt()
                    } catch (e: Exception) {
                        try {
                            val stringValue = cell.stringCellValue?.trim()
                            stringValue?.toIntOrNull() ?: 0
                        } catch (e2: Exception) {
                            log.warn("Error getting formula cell value as int: ${e2.message}")
                            0
                        }
                    }
                }
                CellType.BLANK -> 0
                else -> {
                    log.warn("Cannot convert cell type ${cell.cellType} to int")
                    0
                }
            }
        } catch (e: Exception) {
            log.warn("Error getting cell value as int: ${e.message}")
            0
        }
    }

    private fun getCellValueAsLong(cell: Cell?): Long? {
        if (cell == null) return null

        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toLong()
            CellType.STRING -> cell.stringCellValue?.toLongOrNull()
            CellType.FORMULA -> {
                try {
                    cell.numericCellValue.toLong()
                } catch (e: Exception) {
                    cell.stringCellValue?.toLongOrNull()
                }
            }
            else -> null
        }
    }
}