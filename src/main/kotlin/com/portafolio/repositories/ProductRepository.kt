package com.portafolio.repositories

import com.portafolio.entities.Product
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository: JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.wallet.walletId in ?1")
    fun findAllProductByWallets(walletIds: List<Int>, pageable: Pageable) : List<Product>

}