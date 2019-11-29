package com.portafolio.controllers

import com.portafolio.dtos.ProductResponse
import com.portafolio.repositories.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ProductController {

    @Autowired
    lateinit var repository: ProductRepository

    @GetMapping("/products")
    fun getProducts(pageable: Pageable) : ResponseEntity<ProductResponse> {

        return ResponseEntity.ok().body(ProductResponse(products = repository.findAll(pageable).content));
    }
}