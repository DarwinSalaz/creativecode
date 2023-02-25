package com.portafolio.controllers

import com.portafolio.dtos.CustomerDto
import com.portafolio.dtos.ProductDto
import com.portafolio.dtos.ProductResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.entities.Customer
import com.portafolio.entities.Product
import com.portafolio.mappers.ProductMapper
import com.portafolio.repositories.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class ProductController {

    @Autowired
    lateinit var repository: ProductRepository

    @Autowired
    lateinit var mapper: ProductMapper

    @PostMapping("/products")
    fun getProducts(pageable: Pageable, @Valid @RequestBody walletRequest: WalletRequest?) : ResponseEntity<ProductResponse> {
        var includeWalletName = false
        val products = if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            //repository.findAll(pageable).content
            repository.findAll()
        } else {
            includeWalletName = walletRequest.walletIds.size > 1
            repository.findAllProductByWallets(walletRequest.walletIds, pageable)
        }

        return ResponseEntity.ok().body(ProductResponse(products = mapper.map(products, includeWalletName)));
    }

    @PostMapping("/product/create")
    fun createCustomer(@Valid @RequestBody productDto: ProductDto) : Product {

        return repository.save(mapper.mapRev(productDto))
    }

    @PutMapping("/product/{id_product}")
    fun update(@Valid @RequestBody productDto: ProductDto, @PathVariable("id_product") idProduct : Long) : Product? {
        var product = repository.findById(idProduct).get()

        product = mapper.mapProductUpdate(product, productDto)
        return repository.save(product)
    }

}