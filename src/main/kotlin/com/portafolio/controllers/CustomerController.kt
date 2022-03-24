package com.portafolio.controllers

import com.portafolio.dtos.CustomerDto
import com.portafolio.dtos.CustomersResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.entities.Customer
import com.portafolio.mappers.CustomerMapper
import com.portafolio.repositories.CustomerRepository
import com.portafolio.services.CustomerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
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

    @PutMapping("/customer/{id_customer}")
    fun update(@Valid @RequestBody customerDto: CustomerDto, @PathVariable("id_customer") idCustomer : Long) : Customer? {
        var customer = repository.getOne(idCustomer)

        customer = mapper.map(customerDto, customer)
        return service.save(customer)
    }

    @PostMapping("/customer")
    fun getCustomers(pageable: Pageable, @Valid @RequestBody walletRequest: WalletRequest?) : ResponseEntity<CustomersResponse> {
        val customers = if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            repository.findAll(pageable).content
        } else {
            repository.findAllCustomerByWallets(walletRequest.walletIds, pageable)
        }

        return ResponseEntity.ok().body(CustomersResponse(customers = customers ?: listOf()));
    }

    @GetMapping("/customer/{id_customer}")
    fun getCustomer(@PathVariable("id_customer") idCustomer : Long) = repository.getOne(idCustomer)

}