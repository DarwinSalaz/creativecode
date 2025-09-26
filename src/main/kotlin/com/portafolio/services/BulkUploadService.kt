package com.portafolio.services

import com.portafolio.dtos.*
import com.portafolio.controllers.CustomerController
import com.portafolio.controllers.ServiceController
import com.portafolio.controllers.ProductController
import com.portafolio.entities.Customer
import com.portafolio.entities.Service
import com.portafolio.repositories.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@org.springframework.stereotype.Service
class BulkUploadService {
    
    @Autowired
    lateinit var customerController: CustomerController
    
    @Autowired
    lateinit var serviceController: ServiceController
    
    @Autowired
    lateinit var productController: ProductController
    
    private val DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val DATE_TIME_FORMAT_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    fun processBulkUpload(request: BulkUploadRequest, authToken: String): BulkUploadResult {
        val results = mutableListOf<RecordResult>()
        
        // Obtener productos disponibles usando el walletId del request
        val products = getProducts(request.walletId)
        val productMap = products.associate { it.name.toLowerCase() to it }
        
        for ((index, record) in request.records.withIndex()) {
            try {
                // Crear cliente usando el controlador
                val customerId = registerCustomer(record, request.walletId)
                
                // Crear servicio usando el controlador
                val serviceResult = registerService(record, customerId, productMap, authToken, request.walletId)
                
                results.add(RecordResult(
                    rowNumber = index + 1,
                    success = true,
                    customerId = customerId,
                    serviceId = serviceResult,
                    message = "Registro procesado exitosamente"
                ))
                
            } catch (e: Exception) {
                results.add(RecordResult(
                    rowNumber = index + 1,
                    success = false,
                    customerId = null,
                    serviceId = null,
                    message = "Error: ${e.message}"
                ))
            }
        }
        
        val successCount = results.filter { it.success }.size
        val errorCount = results.filter { !it.success }.size
        
        return BulkUploadResult(
            totalProcessed = results.size,
            successCount = successCount,
            errorCount = errorCount,
            results = results
        )
    }
    
    private fun getProducts(walletId: Int): List<ProductInfo> {
        val walletRequest = WalletRequest(walletIds = listOf(walletId))
        val response = productController.getProducts(Pageable.unpaged(), walletRequest)
        
        if (response.statusCode.is2xxSuccessful) {
            val productResponse = response.body as ProductResponse
            return productResponse.products.map { product ->
                ProductInfo(
                    productId = product.productId,
                    name = product.name,
                    value = product.value.toDouble()
                )
            }
        } else {
            throw Exception("Error fetching products: ${response.statusCode}")
        }
    }
    
    private fun registerCustomer(record: BulkUploadRecord, walletId: Int): Long {
        var cellphone = record.cellphone ?: "0"
        if (cellphone.trim().isEmpty()) {
            cellphone = "0"
        }
        
        val customerDto = CustomerDto(
            companyId = 1,
            name = record.name,
            lastName = record.last_name,
            cellphone = cellphone,
            email = record.email,
            address = record.address,
            identificationNumber = record.identification_number,
            active = true,
            gender = record.gender,
            observation = record.observation,
            walletId = walletId
        )
        
        val response = customerController.createCustomer(customerDto, false)
        
        if (response.statusCode.is2xxSuccessful) {
            val customer = response.body as Customer
            return customer.customerId
        } else {
            throw Exception("Error registering customer: ${response.statusCode} - ${response.body}")
        }
    }
    
    private fun registerService(record: BulkUploadRecord, customerId: Long, productMap: Map<String, ProductInfo>, authToken: String, walletId: Int): Long {
        // Preparar productos del servicio
        val serviceProducts = mutableListOf<ServiceProductDto>()
        val productNames = record.product_name.split("|")
        val productQuantities = record.product_quantity.split("|")
        
        for ((index, productName) in productNames.withIndex()) {
            val cleanName = productName.trim().toLowerCase()
            val productQuantity = productQuantities[index].trim()
            
            // Validación robusta como en el script Python
            val matchedProduct = productMap.entries.find { 
                it.key.trim().toLowerCase() == cleanName 
            }?.value ?: throw IllegalArgumentException("Producto '$productName' no encontrado en la lista de productos")
            
            // Validar cantidad
            val quantity = try {
                val qty = productQuantity.toInt()
                if (qty <= 0) {
                    throw IllegalArgumentException("Cantidad inválida para producto '$productName': $productQuantity")
                }
                qty
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Cantidad inválida para producto '$productName': $productQuantity")
            }
            
            serviceProducts.add(ServiceProductDto(
                productId = matchedProduct.productId,
                value = matchedProduct.value.toBigDecimal(),
                quantity = quantity,
                name = matchedProduct.name
            ))
        }
        
                // Convertir fecha: aceptar MM/dd/yyyy o yyyy-MM-dd HH:mm:ss
                val nextPaymentDateTime = try {
                    LocalDate.parse(record.next_payment_date, DATE_FORMAT_SLASH).atStartOfDay()
                } catch (e: Exception) {
                    LocalDateTime.parse(record.next_payment_date, DATE_TIME_FORMAT_DASH)
                }
        
        val serviceDto = ServiceDto(
            serviceId = 0L, // Se asignará automáticamente
            applicationUserId = record.application_user_id ?: 48L,
            serviceValue = record.valor_servicio.toBigDecimal(),
            downPayment = record.cuota_inicial.toBigDecimal(),
            discount = record.descuento.toBigDecimal(),
            totalValue = record.valor_total.toBigDecimal(),
            debt = record.deuda.toBigDecimal(),
            daysPerFee = record.dias_cuota,
            quantityOfFees = record.nro_cuotas,
            feeValue = record.valor_cuota.toBigDecimal(),
            pendingFees = record.nro_cuotas,
            initialPayment = (record.abono ?: 0.0).toBigDecimal(),
            walletId = walletId,
            customerId = customerId,
            state = "created",
            observations = "cargue_automatico",
            nextPaymentDate = nextPaymentDateTime,
            serviceProducts = serviceProducts
        )
        
        val response = serviceController.createService(serviceDto, authToken)
        
        if (response.statusCode.is2xxSuccessful) {
            val service = response.body as Service
            return service.serviceId
        } else {
            throw Exception("Error registering service: ${response.statusCode} - ${response.body}")
        }
    }
}

data class BulkUploadResult(
    val totalProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<RecordResult>
)

data class RecordResult(
    val rowNumber: Int,
    val success: Boolean,
    val customerId: Long?,
    val serviceId: Long?,
    val message: String
)

data class ProductInfo(
    val productId: Long,
    val name: String,
    val value: Double
)