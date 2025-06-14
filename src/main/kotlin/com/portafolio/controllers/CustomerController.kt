package com.portafolio.controllers

import com.portafolio.dtos.CustomerDto
import com.portafolio.dtos.CustomersResponse
import com.portafolio.dtos.WalletRequest
import com.portafolio.entities.Customer
import com.portafolio.mappers.CustomerMapper
import com.portafolio.repositories.CustomerRepository
import com.portafolio.services.CustomerService
import com.portafolio.services.ServicesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
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

    @Autowired
    lateinit var servicesService: ServicesService

    @PostMapping("/customer/create")
    fun createCustomer(
        @Valid @RequestBody customerDto: CustomerDto,
        @RequestHeader(value = "force_save", required = false, defaultValue = "false") forceSave: Boolean
    ) : ResponseEntity<Any> {
        val identificationNumber = customerDto.identificationNumber

        // Validar si el número de identificación está vacío
        if (identificationNumber.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El número de identificación es obligatorio.")
        }

        val customers = repository.findAllByIdentificationNumber(identificationNumber)

        if (!customers.isNullOrEmpty()) {
            val customerMatch = customers.firstOrNull { it.walletId == customerDto.walletId }

            if (customerMatch != null) {
                return ResponseEntity.ok(customerMatch)
            } else {
                if (!forceSave) {
                    val inDebt = servicesService.isCustomerInDebit(customers[0].customerId)

                    val response = mapOf(
                        "error" to true,
                        "message" to "El cliente ya existe en otra cartera.",
                        "in_debt" to inDebt
                    )

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
                }
            }
        }

        val createdCustomer = service.save(mapper.map(customerDto))

        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer)
    }

    @PutMapping("/customer/{id_customer}")
    fun update(@Valid @RequestBody customerDto: CustomerDto, @PathVariable("id_customer") idCustomer : Long) : Customer? {
        var customer = repository.getOne(idCustomer)

        customer = mapper.map(customerDto, customer)
        return service.save(customer)
    }

    @PostMapping("/customer")
    fun getCustomers(@PageableDefault(size = 1000) pageable: Pageable, @Valid @RequestBody walletRequest: WalletRequest?) : ResponseEntity<CustomersResponse> {
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