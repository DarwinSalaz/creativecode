package com.portafolio.repositories

import com.portafolio.entities.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
interface CustomerRepository: JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.walletId in ?1")
    fun findAllCustomerByWallets(walletIds: List<Int>, pageable: Pageable) : List<Customer>?

    fun findAllByIdentificationNumber(identificationNumber: String): List<Customer>

    @Query("SELECT c FROM Customer c WHERE c.identificationNumber = ?1 AND c.walletId = ?2")
    fun findCustomerByIdAndWallet(identificationNumber: String, walletId: Int) : List<Customer>?
}