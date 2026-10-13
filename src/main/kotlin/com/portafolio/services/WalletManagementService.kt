package com.portafolio.services

import com.portafolio.dtos.WalletManagementResponse
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.RelUserWallet
import com.portafolio.entities.Wallet
import com.portafolio.repositories.CompanyRepository
import com.portafolio.repositories.RelUserWalletRepository
import com.portafolio.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WalletManagementService(
    private val walletRepository: WalletRepository,
    private val companyRepository: CompanyRepository,
    private val relUserWalletRepository: RelUserWalletRepository
) {
    fun list(administrator: ApplicationUser): List<WalletManagementResponse> {
        validateAdministrator(administrator)
        return walletRepository.findAllByCompanyCompanyIdOrderByNameAsc(administrator.companyId).map(::response)
    }

    @Transactional
    fun create(name: String, administrator: ApplicationUser): WalletManagementResponse {
        validateAdministrator(administrator)
        val normalizedName = normalizeName(name)
        if (walletRepository.existsByCompanyCompanyIdAndNameIgnoreCase(administrator.companyId, normalizedName)) {
            throw IllegalArgumentException("Ya existe una cartera con ese nombre.")
        }
        val company = companyRepository.findById(administrator.companyId)
            .orElseThrow { IllegalArgumentException("La empresa del usuario no existe.") }
        val wallet = walletRepository.save(Wallet(company = company, name = normalizedName, active = true))
        relUserWalletRepository.save(RelUserWallet(wallet.walletId, administrator.applicationUserId))
        return response(wallet)
    }

    @Transactional
    fun rename(walletId: Int, name: String, administrator: ApplicationUser): WalletManagementResponse {
        validateAdministrator(administrator)
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { IllegalArgumentException("La cartera no existe.") }
        if (wallet.company.companyId != administrator.companyId) {
            throw SecurityException("No puede modificar una cartera de otra empresa.")
        }
        val normalizedName = normalizeName(name)
        if (walletRepository.existsByCompanyCompanyIdAndNameIgnoreCaseAndWalletIdNot(
                administrator.companyId, normalizedName, walletId)) {
            throw IllegalArgumentException("Ya existe una cartera con ese nombre.")
        }
        wallet.name = normalizedName
        return response(walletRepository.save(wallet))
    }

    private fun validateAdministrator(user: ApplicationUser) {
        if (user.userProfileId != 1) {
            throw SecurityException("Esta operación solo está disponible para administradores.")
        }
    }

    private fun normalizeName(name: String): String {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) throw IllegalArgumentException("El nombre de la cartera es obligatorio.")
        if (normalized.length > 100) throw IllegalArgumentException("El nombre de la cartera no puede superar 100 caracteres.")
        return normalized
    }

    private fun response(wallet: Wallet) = WalletManagementResponse(wallet.walletId, wallet.name, wallet.active)
}
