package com.portafolio.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.Company
import com.portafolio.entities.RelUserWallet
import com.portafolio.entities.Wallet
import com.portafolio.repositories.CompanyRepository
import com.portafolio.repositories.RelUserWalletRepository
import com.portafolio.repositories.WalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Optional

class WalletManagementServiceTest {
    private val walletRepository: WalletRepository = mock()
    private val companyRepository: CompanyRepository = mock()
    private val relUserWalletRepository: RelUserWalletRepository = mock()
    private lateinit var service: WalletManagementService
    private val company = Company(1, "Empresa", "123", true)
    private val administrator = ApplicationUser(8, 1, "admin", "Admin", password = "secret", userProfileId = 1, active = true)

    @Before
    fun setUp() {
        service = WalletManagementService(walletRepository, companyRepository, relUserWalletRepository)
    }

    @Test
    fun `creates wallet in administrators company and assigns its creator`() {
        whenever(companyRepository.findById(1)).thenReturn(Optional.of(company))
        whenever(walletRepository.save(any<Wallet>())).thenAnswer { (it.arguments[0] as Wallet).copy(walletId = 25) }

        val result = service.create("  Cartera   Norte  ", administrator)

        assertEquals(25, result.walletId)
        assertEquals("Cartera Norte", result.name)
        verify(relUserWalletRepository).save(RelUserWallet(25, administrator.applicationUserId))
    }

    @Test
    fun `rejects wallet management for non administrators`() {
        try {
            service.list(administrator.copy(userProfileId = 3))
            fail("Expected SecurityException")
        } catch (error: SecurityException) {
            assertEquals("Esta operación solo está disponible para administradores.", error.message)
        }
    }

    @Test
    fun `does not rename wallets from another company`() {
        whenever(walletRepository.findById(25)).thenReturn(Optional.of(Wallet(25, Company(2, "Otra", "456", true), "Externa", true)))
        try {
            service.rename(25, "Nuevo nombre", administrator)
            fail("Expected SecurityException")
        } catch (error: SecurityException) {
            assertEquals("No puede modificar una cartera de otra empresa.", error.message)
        }
    }
}
