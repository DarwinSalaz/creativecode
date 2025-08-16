package com.portafolio.controllers

import com.portafolio.dtos.WalletRequest
import com.portafolio.entities.Wallet
import com.portafolio.repositories.WalletRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS], allowedHeaders = ["*"])
class WalletController {

    @Autowired
    lateinit var repository: WalletRepository

    @PostMapping("/wallets")
    fun getAll(@Valid @RequestBody walletRequest: WalletRequest?) : List<Wallet> {

        return if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            repository.findAll()
        } else {
            repository.findAllById(walletRequest.walletIds)
        }
    }

}