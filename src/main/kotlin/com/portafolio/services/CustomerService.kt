package com.portafolio.services

import com.portafolio.dtos.CustomerDto
import com.portafolio.entities.Customer
import com.portafolio.repositories.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CustomerService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: CustomerRepository

    fun save(customer: Customer) : Customer {

        return repository.save(customer)
    }

    fun update(customer: Customer) : Customer {

        return repository.save(customer)
    }

}