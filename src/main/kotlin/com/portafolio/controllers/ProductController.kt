package com.portafolio.controllers

import com.portafolio.dtos.ProductResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.mappers.ProductMapper
import com.portafolio.repositories.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ProductController {

    @Autowired
    lateinit var repository: ProductRepository

    @Autowired
    lateinit var mapper: ProductMapper

    @PostMapping("/products")
    fun getProducts(pageable: Pageable, @Valid @RequestBody walletRequest: WalletRequest?) : ResponseEntity<ProductResponse> {
        var includeWalletName = false
        val products = if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            repository.findAll(pageable).content
        } else {
            includeWalletName = walletRequest.walletIds.size > 1
            repository.findAllProductByWallets(walletRequest.walletIds, pageable)
        }

        return ResponseEntity.ok().body(ProductResponse(products = mapper.map(products, includeWalletName)));
    }
}