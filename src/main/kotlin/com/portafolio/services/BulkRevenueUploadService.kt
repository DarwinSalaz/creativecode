package com.portafolio.services

import com.portafolio.dtos.*
import com.portafolio.entities.Revenue
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class BulkRevenueUploadService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var revenueService: RevenueService

    @Autowired
    lateinit var validationService: BulkRevenueUploadValidationService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    fun processBulkRevenueUpload(request: BulkRevenueUploadRequest, username: String): BulkRevenueUploadResult {
        log.info("Processing bulk revenue upload for ${request.records.size} records")

        val results = mutableListOf<RevenueRecordResult>()
        
        // Obtener usuario
        val user = applicationUserRepository.findByUsername(username)
            ?: throw IllegalArgumentException("Usuario no encontrado: $username")

        val applicationUserId = user.applicationUserId

        for ((index, record) in request.records.withIndex()) {
            val rowNumber = index + 2 // +2 porque empieza en 1 y la primera es header
            
            try {
                // Parsear fecha
                val revenueDate = validationService.parseRevenueDate(record.revenueDate)
                
                // Crear entidad Revenue
                val revenue = Revenue(
                    applicationUserId = applicationUserId,
                    revenueType = record.revenueType,
                    value = record.value.toBigDecimal(),
                    revenueDate = revenueDate,
                    justification = record.justification,
                    walletId = request.walletId
                )

                // Reutilizar la lógica existente de RevenueService
                val savedRevenue = revenueService.save(revenue)

                results.add(RevenueRecordResult(
                    rowNumber = rowNumber,
                    success = true,
                    revenueId = savedRevenue.revenueId,
                    message = "Ingreso registrado exitosamente"
                ))

                log.info("Revenue created successfully for row $rowNumber: ${savedRevenue.revenueId}")

            } catch (e: Exception) {
                log.error("Error processing revenue for row $rowNumber: ${e.message}", e)
                results.add(RevenueRecordResult(
                    rowNumber = rowNumber,
                    success = false,
                    revenueId = null,
                    message = "Error: ${e.message}"
                ))
            }
        }

        val successCount = results.count { it.success }
        val errorCount = results.count { !it.success }

        log.info("Bulk revenue upload completed: $successCount successful, $errorCount errors")

        return BulkRevenueUploadResult(
            totalProcessed = results.size,
            successCount = successCount,
            errorCount = errorCount,
            results = results
        )
    }
}

