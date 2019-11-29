package com.portafolio.controllers

import com.portafolio.dtos.CustomerDto
import com.portafolio.dtos.CustomersResponse
import com.portafolio.entities.Customer
import com.portafolio.mappers.CustomerMapper
import com.portafolio.repositories.CustomerRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.CustomerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class CustomerController {

    @Autowired
    lateinit var service: CustomerService

    @Autowired
    lateinit var mapper: CustomerMapper

    @Autowired
    lateinit var repository: CustomerRepository

    @PostMapping("/customer/create")
    fun createCustomer(@Valid @RequestBody customerDto: CustomerDto) : Customer {

        return service.save(mapper.map(customerDto))
    }

    @GetMapping("/customer")
    fun getCustomers(pageable: Pageable) : ResponseEntity<CustomersResponse> {

        return ResponseEntity.ok().body(CustomersResponse(customers = repository.findAll(pageable).content));
    }

    @GetMapping("/customer/{id_customer}")
    fun getCustomer(@PathVariable("id_customer") idCustomer : Long) = repository.getOne(idCustomer)

}