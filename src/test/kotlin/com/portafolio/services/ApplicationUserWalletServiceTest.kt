package com.portafolio.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.portafolio.entities.RelUserWallet
import com.portafolio.repositories.RelUserWalletRepository
import org.junit.Test

class ApplicationUserWalletServiceTest {

    private val repository: RelUserWalletRepository = mock()
    private val service = ApplicationUserWalletService(repository)

    @Test
    fun `replaces existing wallets with selected wallets`() {
        service.replaceWallets(12, listOf(4, 7, 4))

        verify(repository).deleteAllWalletsByUser(12)
        verify(repository).saveAll(eq(listOf(
            RelUserWallet(walletId = 4, applicationUserId = 12),
            RelUserWallet(walletId = 7, applicationUserId = 12)
        )))
    }

    @Test
    fun `removes every wallet when selection is empty`() {
        service.replaceWallets(12, emptyList())

        verify(repository).deleteAllWalletsByUser(12)
        verify(repository, never()).saveAll(any<List<RelUserWallet>>())
    }

    @Test
    fun `removes every wallet when selection is null`() {
        service.replaceWallets(12, null)

        verify(repository).deleteAllWalletsByUser(12)
        verify(repository, never()).saveAll(any<List<RelUserWallet>>())
    }
}
