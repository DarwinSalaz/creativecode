package com.portafolio.controllers

import com.portafolio.dtos.WalletRequest
import com.portafolio.dtos.WalletNameRequest
import com.portafolio.entities.Wallet
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.repositories.WalletRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.WalletManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS], allowedHeaders = ["*"])
class WalletController {

    @Autowired
    lateinit var repository: WalletRepository

    @Autowired
    lateinit var applicationUserService: ApplicationUserService

    @Autowired
    lateinit var applicationUserRepository: ApplicationUserRepository

    @Autowired
    lateinit var walletManagementService: WalletManagementService

    @PostMapping("/wallets")
    fun getAll(@Valid @RequestBody walletRequest: WalletRequest?) : List<Wallet> {

        return if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            repository.findAll()
        } else {
            repository.findAllById(walletRequest.walletIds)
        }
    }

    @GetMapping("/wallets/manage")
    fun listManagedWallets(@RequestHeader("Authorization") authorization: String): ResponseEntity<Any> =
        withAdministrator(authorization) { administrator -> walletManagementService.list(administrator) }

    @PostMapping("/wallets/manage")
    fun createWallet(
        @Valid @RequestBody request: WalletNameRequest,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> = withAdministrator(authorization) { administrator ->
        walletManagementService.create(request.name, administrator)
    }

    @PutMapping("/wallets/manage/{wallet_id}")
    fun renameWallet(
        @PathVariable("wallet_id") walletId: Int,
        @Valid @RequestBody request: WalletNameRequest,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Any> = withAdministrator(authorization) { administrator ->
        walletManagementService.rename(walletId, request.name, administrator)
    }

    private fun withAdministrator(
        authorization: String,
        operation: (com.portafolio.entities.ApplicationUser) -> Any
    ): ResponseEntity<Any> {
        return try {
            val token = if (authorization.startsWith("Bearer ")) authorization.substring(7) else authorization
            val username = applicationUserService.verifyToken(token)
            val user = applicationUserRepository.findByUsername(username)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "Usuario no autorizado."))
            if (user.userProfileId != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("message" to "Esta operación solo está disponible para administradores."))
            }
            ResponseEntity.ok(operation(user))
        } catch (error: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to (error.message ?: "Operación no autorizada.")))
        } catch (error: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (error.message ?: "Solicitud inválida.")))
        }
    }

}
