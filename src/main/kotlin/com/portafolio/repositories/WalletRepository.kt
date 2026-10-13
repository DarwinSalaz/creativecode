package com.portafolio.repositories

import com.portafolio.entities.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository: JpaRepository<Wallet, Int> {
    fun findAllByCompanyCompanyIdOrderByNameAsc(companyId: Int): List<Wallet>
    fun existsByCompanyCompanyIdAndNameIgnoreCase(companyId: Int, name: String): Boolean
    fun existsByCompanyCompanyIdAndNameIgnoreCaseAndWalletIdNot(companyId: Int, name: String, walletId: Int): Boolean
}
