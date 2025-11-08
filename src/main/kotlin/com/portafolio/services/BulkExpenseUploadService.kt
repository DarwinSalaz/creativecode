package com.portafolio.services

import com.portafolio.dtos.*
import com.portafolio.entities.Expense
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class BulkExpenseUploadService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var expenseService: ExpenseService

    @Autowired
    lateinit var validationService: BulkExpenseUploadValidationService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    fun processBulkExpenseUpload(request: BulkExpenseUploadRequest, username: String): BulkExpenseUploadResult {
        log.info("Processing bulk expense upload for ${request.records.size} records")

        val results = mutableListOf<ExpenseRecordResult>()
        
        // Obtener usuario
        val user = applicationUserRepository.findByUsername(username)
            ?: throw IllegalArgumentException("Usuario no encontrado: $username")

        val applicationUserId = user.applicationUserId

        for ((index, record) in request.records.withIndex()) {
            val rowNumber = index + 2 // +2 porque empieza en 1 y la primera es header
            
            try {
                // Parsear fecha
                val expenseDate = validationService.parseExpenseDate(record.expenseDate)
                
                // Crear entidad Expense
                val expense = Expense(
                    applicationUserId = applicationUserId,
                    expenseType = record.expenseType,
                    value = record.value.toBigDecimal(),
                    expenseDate = expenseDate,
                    justification = record.justification,
                    walletId = request.walletId
                )

                // Reutilizar la lógica existente de ExpenseService
                val savedExpense = expenseService.save(expense)

                results.add(ExpenseRecordResult(
                    rowNumber = rowNumber,
                    success = true,
                    expenseId = savedExpense.expenseId,
                    message = "Gasto registrado exitosamente"
                ))

                log.info("Expense created successfully for row $rowNumber: ${savedExpense.expenseId}")

            } catch (e: Exception) {
                log.error("Error processing expense for row $rowNumber: ${e.message}", e)
                results.add(ExpenseRecordResult(
                    rowNumber = rowNumber,
                    success = false,
                    expenseId = null,
                    message = "Error: ${e.message}"
                ))
            }
        }

        val successCount = results.count { it.success }
        val errorCount = results.count { !it.success }

        log.info("Bulk expense upload completed: $successCount successful, $errorCount errors")

        return BulkExpenseUploadResult(
            totalProcessed = results.size,
            successCount = successCount,
            errorCount = errorCount,
            results = results
        )
    }
}

