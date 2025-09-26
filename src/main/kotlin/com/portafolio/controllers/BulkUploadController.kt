package com.portafolio.controllers

import com.portafolio.dtos.*
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

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class BulkUploadController {
    
    @Autowired
    lateinit var validationService: BulkUploadValidationService
    
    @Autowired
    lateinit var bulkUploadService: BulkUploadService
    
    @Autowired
    lateinit var applicationUserService: ApplicationUserService
    
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
            //val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            //val username = applicationUserService.verifyToken(token)
            
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
        
        // Saltar la primera fila (headers)
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            
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
            } catch (e: Exception) {
                // Saltar filas con errores de parsing
                continue
            }
        }
        
        workbook.close()
        return records
    }
    
    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                // Detectar fechas en celdas numéricas de Excel y convertir a MM/dd/yyyy
                if (DateUtil.isCellDateFormatted(cell)) {
                    val sdf = SimpleDateFormat("MM/dd/yyyy")
                    return sdf.format(cell.dateCellValue)
                }
                cell.numericCellValue.toString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    // Si la celda de fórmula es fecha, formatear como fecha
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val sdf = SimpleDateFormat("MM/dd/yyyy")
                        return sdf.format(cell.dateCellValue)
                    }
                    cell.stringCellValue ?: ""
                } catch (e: Exception) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val sdf = SimpleDateFormat("MM/dd/yyyy")
                        return sdf.format(cell.dateCellValue)
                    }
                    cell.numericCellValue.toString()
                }
            }
            else -> ""
        }
    }
    
    private fun getCellValueAsDouble(cell: Cell?): Double {
        if (cell == null) return 0.0
        
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue?.toDoubleOrNull() ?: 0.0
            CellType.FORMULA -> {
                try {
                    cell.numericCellValue
                } catch (e: Exception) {
                    cell.stringCellValue?.toDoubleOrNull() ?: 0.0
                }
            }
            else -> 0.0
        }
    }
    
    private fun getCellValueAsInt(cell: Cell?): Int {
        if (cell == null) return 0
        
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue?.toIntOrNull() ?: 0
            CellType.FORMULA -> {
                try {
                    cell.numericCellValue.toInt()
                } catch (e: Exception) {
                    cell.stringCellValue?.toIntOrNull() ?: 0
                }
            }
            else -> 0
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