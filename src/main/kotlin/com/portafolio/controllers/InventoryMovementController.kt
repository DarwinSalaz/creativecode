package com.portafolio.controllers

import com.portafolio.dtos.InventoryMovementRequest
import com.portafolio.dtos.InventoryMovementResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.services.InventoryMovementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
@RequestMapping("/api/portfolio/inventory")
class InventoryMovementController {
    
    @Autowired
    lateinit var inventoryMovementService: InventoryMovementService
    
    @PostMapping("/movement")
    fun registerMovement(
        @Valid @RequestBody request: InventoryMovementRequest,
        @RequestHeader("user-id") userId: Long,
        @RequestHeader("username") username: String
    ): ResponseEntity<InventoryMovementResponse> {
        try {
            val response = inventoryMovementService.registerMovement(request, userId, username)
            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }
    
    @PostMapping("/movements")
    fun getMovements(@Valid @RequestBody walletRequest: WalletRequest): ResponseEntity<List<InventoryMovementResponse>> {
        val movements = inventoryMovementService.getMovementsByWalletIds(walletRequest.walletIds!!)
        return ResponseEntity.ok(movements)
    }
    
    @GetMapping("/movements/product/{productId}")
    fun getMovementsByProduct(@PathVariable productId: Long): ResponseEntity<List<InventoryMovementResponse>> {
        val movements = inventoryMovementService.getMovementsByProductId(productId)
        return ResponseEntity.ok(movements)
    }
    
    @PostMapping("/movements/date-range")
    fun getMovementsByDateRange(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @Valid @RequestBody walletRequest: WalletRequest
    ): ResponseEntity<List<InventoryMovementResponse>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val start = LocalDateTime.parse(startDate + "T00:00:00")
        val end = LocalDateTime.parse(endDate + "T23:59:59")
        
        val movements = inventoryMovementService.getMovementsByDateRange(walletRequest.walletIds!!, start, end)
        return ResponseEntity.ok(movements)
    }
} 