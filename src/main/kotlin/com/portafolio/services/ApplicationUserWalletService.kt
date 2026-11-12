package com.portafolio.services

import com.portafolio.entities.RelUserWallet
import com.portafolio.repositories.RelUserWalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplicationUserWalletService(
    private val relUserWalletRepository: RelUserWalletRepository
) {

    @Transactional
    fun replaceWallets(applicationUserId: Long, walletIds: List<Int>?) {
        relUserWalletRepository.deleteAllWalletsByUser(applicationUserId)

        val relations = walletIds.orEmpty().distinct().map { walletId ->
            RelUserWallet(walletId = walletId, applicationUserId = applicationUserId)
        }
        if (relations.isNotEmpty()) {
            relUserWalletRepository.saveAll(relations)
        }
    }
}
