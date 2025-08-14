package com.portafolio.controllers

import com.portafolio.dtos.InventoryMovementRequest
import com.portafolio.dtos.InventoryMovementResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.InventoryMovementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository
    
    @PostMapping("/movement")
    fun registerMovement(
        @Valid @RequestBody request: InventoryMovementRequest,
        @RequestHeader("username") username: String,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<InventoryMovementResponse> {
        try {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            val applicationUsername: String = applicationUserService.verifyToken(token)
            val user = applicationUserRepository.findByUsername(applicationUsername)

            user ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            // Extraer userId del token JWT (Bearer token)
            val userId = user.applicationUserId
            val response = inventoryMovementService.registerMovement(request, userId, username)
            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            return ResponseEntity.status(401).build() // Unauthorized
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