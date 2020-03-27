package com.portafolio.mappers

import com.portafolio.dtos.CustomerDto
import com.portafolio.entities.Customer
import com.portafolio.repositories.CompanyRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CustomerMapper {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val listFemaleIcons = listOf<String>("av-3.png", "av-5.png", "av-7.png")
    private val listMascIcons = listOf<String>("av-1.png", "av-2.png", "av-4.png", "av-6.png", "av-8.png")

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    fun map(customerDto: CustomerDto) : Customer {
        val customer : Customer
        val optional = companyRepository.findById(customerDto.companyId)
        val companyResult = optional.get()
        val iconCustomer = if ("f".equals(customerDto.gender)) listFemaleIcons.random() else listMascIcons.random()
        customer = Customer (
            name = customerDto.name,
            company = companyResult,
            lastName = customerDto.lastName,
            address = customerDto.address,
            cellphone = customerDto.cellphone,
            email = customerDto.email,
            identificationNumber = customerDto.identificationNumber,
            gender = customerDto.gender,
            icon = iconCustomer,
            observation = customerDto.observation,
            walletId = customerDto.walletId
        )

        return customer
    }

    fun map(customerDto: CustomerDto, customer: Customer) : Customer {
        customer.name = customerDto.name
        customer.lastName = customerDto.lastName ?: customer.lastName
        customer.address = customerDto.address ?: customer.address
        customer.cellphone = customerDto.cellphone ?: customer.cellphone
        customer.email = customerDto.email ?: customer.email
        customer.identificationNumber = customerDto.identificationNumber ?: customer.identificationNumber
        customer.gender = customerDto.gender ?: customer.gender
        customer.observation = customerDto.observation ?: customer.observation
        customer.walletId = customerDto.walletId

        return customer
    }

}