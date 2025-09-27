package com.portafolio.services

import com.portafolio.dtos.*
import com.portafolio.repositories.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@Service
class BulkUploadValidationService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var productRepository: ProductRepository

    private val MAX_RECORDS = 500
    private val DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val DATE_TIME_FORMAT_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun validateBulkUpload(request: BulkUploadRequest): BulkUploadValidationResponse {
        log.info("Starting bulk upload validation for ${request.records.size} records with wallet ID ${request.walletId}")

        val errors = mutableListOf<RecordError>()
        var validRecords = 0
        var totalCustomers = 0
        var totalServices = 0
        var totalProducts = 0
        var estimatedValue = 0.0

        // Validar límite de registros
        if (request.records.size > MAX_RECORDS) {
            errors.add(RecordError(
                rowNumber = 0,
                field = "records",
                message = "El archivo excede el límite máximo de $MAX_RECORDS registros",
                record = BulkUploadRecord("", "", "", "", "", "", "", "", 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0.0, "", 0L, "", "")
            ))
            return BulkUploadValidationResponse(
                isValid = false,
                totalRecords = request.records.size,
                validRecords = 0,
                invalidRecords = request.records.size,
                errors = errors,
                summary = UploadSummary(0, 0, 0, 0.0)
            )
        }

        // Obtener productos disponibles usando el walletId del request
        log.info("Fetching products for wallet ID: ${request.walletId}")
        val products = try {
            productRepository.findAllProductByWallets(listOf(request.walletId), Pageable.unpaged())
        } catch (e: Exception) {
            log.error("Error fetching products for wallet ${request.walletId}: ${e.message}", e)
            throw e
        }

        log.info("Found ${products.size} products for wallet ${request.walletId}")
        val productMap = products.associate { it.name.toLowerCase() to it }
        log.info("Product map created with ${productMap.size} entries")

        // Lista para recopilar productos faltantes
        val missingProducts = mutableSetOf<String>()

        for ((index, record) in request.records.withIndex()) {
            log.info("Validating record ${index + 1}/${request.records.size}")
            val recordErrors = mutableListOf<String>()

            // Validar campos de cliente
            if (record.name.isBlank()) {
                recordErrors.add("El campo 'name' es obligatorio")
            }

            // Validar campos numéricos del servicio
            val numericFields = mapOf(
                "valor_servicio" to record.valor_servicio,
                "cuota_inicial" to record.cuota_inicial,
                "descuento" to record.descuento,
                "deuda" to record.deuda,
                "valor_total" to record.valor_total,
                "dias_cuota" to record.dias_cuota.toDouble(),
                "nro_cuotas" to record.nro_cuotas.toDouble(),
                "valor_cuota" to record.valor_cuota
            )

            numericFields.forEach { (field, value) ->
                if (field != "descuento" && field != "deuda" && value <= 0) {
                    recordErrors.add("El campo '$field' debe ser mayor que 0")
                }
                if (value.isInfinite() || value.isNaN()) {
                    recordErrors.add("El campo '$field' tiene un valor inválido")
                }
            }

            // Validar fecha: aceptar MM/dd/yyyy o yyyy-MM-dd HH:mm:ss
            var isValidDate = false
            try {
                LocalDate.parse(record.next_payment_date, DATE_FORMAT_SLASH)
                isValidDate = true
            } catch (e: Exception) { /* ignore */ }
            if (!isValidDate) {
                try {
                    LocalDateTime.parse(record.next_payment_date, DATE_TIME_FORMAT_DASH)
                    isValidDate = true
                } catch (e: Exception) { /* ignore */ }
            }
            if (!isValidDate) {
                recordErrors.add("El formato de fecha debe ser MM/dd/yyyy o yyyy-MM-dd HH:mm:ss")
            }

            // Validar productos
            log.info("Validating products for record ${index + 1}: '${record.product_name}' with quantities '${record.product_quantity}'")
            val productNames = record.product_name.split("|")
            val productQuantities = record.product_quantity.split("|")
            log.info("Split into ${productNames.size} products and ${productQuantities.size} quantities")

            if (productNames.size != productQuantities.size) {
                recordErrors.add("El número de productos no coincide con el número de cantidades")
            }

            for ((i, productName) in productNames.withIndex()) {
                log.info("Validating product ${i + 1}: '$productName'")
                val cleanName = productName.trim().toLowerCase()

                if (i >= productQuantities.size) {
                    log.error("Product quantity index $i is out of bounds for productQuantities size ${productQuantities.size}")
                    recordErrors.add("Cantidad faltante para producto '$productName'")
                    continue
                }

                val productQuantity = productQuantities[i].trim()
                log.info("Clean name: '$cleanName', quantity: '$productQuantity'")

                // Validación robusta como en el script Python
                val matchedProduct = productMap.entries.find {
                    it.key.trim().toLowerCase() == cleanName
                }

                if (matchedProduct == null) {
                    log.warn("Product not found: '$productName' (clean: '$cleanName')")
                    missingProducts.add(productName.trim())
                    recordErrors.add("Producto '$productName' no encontrado en el catálogo")
                }

                try {
                    val quantity = productQuantity.toInt()
                    if (quantity <= 0) {
                        recordErrors.add("Cantidad inválida para producto '$productName': $productQuantity")
                    }
                } catch (e: NumberFormatException) {
                    recordErrors.add("Cantidad inválida para producto '$productName': $productQuantity")
                }
            }

            if (recordErrors.isEmpty()) {
                validRecords++
                totalCustomers++
                totalServices++
                totalProducts += productNames.size
                estimatedValue += record.valor_total
            } else {
                recordErrors.forEach { error ->
                    errors.add(RecordError(
                        rowNumber = index + 1,
                        field = "record",
                        message = error,
                        record = record
                    ))
                }
            }
        }

        // Si hay productos faltantes, retornar error específico
        if (missingProducts.isNotEmpty()) {
            log.error("Missing products found: ${missingProducts.joinToString(", ")}")

            val missingProductsError = RecordError(
                rowNumber = 0,
                field = "productos",
                message = "Los siguientes productos no existen en la cartera y deben ser creados primero: ${missingProducts.joinToString(", ")}",
                record = BulkUploadRecord("", "", "", "", "", "", "", "", 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0.0, "", 0L, "", "")
            )

            return BulkUploadValidationResponse(
                isValid = false,
                totalRecords = request.records.size,
                validRecords = 0,
                invalidRecords = request.records.size,
                errors = listOf(missingProductsError),
                summary = UploadSummary(0, 0, 0, 0.0)
            )
        }

        return BulkUploadValidationResponse(
            isValid = errors.isEmpty(),
            totalRecords = request.records.size,
            validRecords = validRecords,
            invalidRecords = request.records.size - validRecords,
            errors = errors,
            summary = UploadSummary(
                totalCustomers = totalCustomers,
                totalServices = totalServices,
                totalProducts = totalProducts,
                estimatedValue = estimatedValue
            )
        )
    }
}