package com.portafolio.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.dtos.InventoryMovementBatchItem
import com.portafolio.dtos.InventoryMovementBatchRequest
import com.portafolio.dtos.InventoryTransferRequest
import com.portafolio.entities.Company
import com.portafolio.entities.InventoryMovement
import com.portafolio.entities.MovementType
import com.portafolio.entities.Product
import com.portafolio.entities.Wallet
import com.portafolio.repositories.InventoryMovementRepository
import com.portafolio.repositories.ProductRepository
import com.portafolio.repositories.WalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Optional

class InventoryMovementServiceTest {
    private val movementRepository: InventoryMovementRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val walletRepository: WalletRepository = mock()
    private lateinit var service: InventoryMovementService
    private val company = Company(1, "Empresa", "123", true)
    private val wallet = Wallet(7, company, "Principal", true)
    private val firstProduct = Product(11, company, "Mesa", null, BigDecimal("100"),
        wallet = wallet, leftQuantity = 3)
    private val secondProduct = Product(12, company, "Silla", null, BigDecimal("50"),
        wallet = wallet, leftQuantity = 8)

    @Before
    fun setUp() {
        firstProduct.leftQuantity = 3
        secondProduct.leftQuantity = 8
        service = InventoryMovementService(movementRepository, productRepository, walletRepository)
        whenever(walletRepository.findById(wallet.walletId)).thenReturn(Optional.of(wallet))
        whenever(productRepository.findById(firstProduct.productId)).thenReturn(Optional.of(firstProduct))
        whenever(productRepository.findById(secondProduct.productId)).thenReturn(Optional.of(secondProduct))
        whenever(movementRepository.save(any<InventoryMovement>())).thenAnswer { invocation ->
            invocation.arguments[0] as InventoryMovement
        }
        whenever(productRepository.save(any<Product>())).thenAnswer { invocation -> invocation.arguments[0] as Product }
    }

    @Test
    fun `registers every batch item and updates its stock`() {
        val result = service.registerMovements(request(), 5, "admin")

        assertEquals(2, result.size)
        assertEquals(7, firstProduct.leftQuantity)
        assertEquals(10, secondProduct.leftQuantity)
        assertEquals(4, result[0].quantity)
        assertEquals(2, result[1].quantity)
        verify(movementRepository, com.nhaarman.mockitokotlin2.times(2)).save(any<InventoryMovement>())
    }

    @Test
    fun `rejects products from another wallet before creating movements`() {
        val otherWallet = Wallet(9, company, "Otra", true)
        val externalProduct = Product(20, company, "Externo", null, BigDecimal.TEN,
            wallet = otherWallet, leftQuantity = 1)
        whenever(productRepository.findById(externalProduct.productId)).thenReturn(Optional.of(externalProduct))

        try {
            service.registerMovements(
                request(listOf(InventoryMovementBatchItem(externalProduct.productId, 1))), 5, "admin")
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals("El producto Externo no pertenece a la cartera seleccionada", error.message)
        }

        verify(movementRepository, never()).save(any<InventoryMovement>())
    }

    @Test
    fun `transfers stock between wallets using equivalent target product`() {
        val targetWallet = Wallet(9, company, "Destino", true)
        val targetProduct = Product(21, company, "Mesa", null, BigDecimal("100"),
            wallet = targetWallet, leftQuantity = 2)
        firstProduct.leftQuantity = 10
        whenever(walletRepository.findById(targetWallet.walletId)).thenReturn(Optional.of(targetWallet))
        whenever(productRepository.findEquivalentProducts(targetWallet.walletId, company.companyId, firstProduct.name))
            .thenReturn(listOf(targetProduct))
        whenever(productRepository.findById(targetProduct.productId)).thenReturn(Optional.of(targetProduct))

        val result = service.transferInventory(
            InventoryTransferRequest(wallet.walletId, targetWallet.walletId, "Reposición", listOf(
                InventoryMovementBatchItem(firstProduct.productId, 4)
            )),
            5,
            "admin"
        )

        assertEquals(6, firstProduct.leftQuantity)
        assertEquals(6, targetProduct.leftQuantity)
        assertEquals(2, result.movements.size)
        assertEquals(MovementType.SALIDA, result.movements[0].movementType)
        assertEquals(MovementType.ENTRADA, result.movements[1].movementType)
    }

    @Test
    fun `rejects transfer when source stock is insufficient`() {
        val targetWallet = Wallet(9, company, "Destino", true)
        whenever(walletRepository.findById(targetWallet.walletId)).thenReturn(Optional.of(targetWallet))

        try {
            service.transferInventory(
                InventoryTransferRequest(wallet.walletId, targetWallet.walletId, null, listOf(
                    InventoryMovementBatchItem(firstProduct.productId, 4)
                )),
                5,
                "admin"
            )
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            assertEquals("Stock insuficiente para Mesa. Disponible: 3", error.message)
        }

        verify(movementRepository, never()).save(any<InventoryMovement>())
    }

    private fun request(items: List<InventoryMovementBatchItem> = listOf(
        InventoryMovementBatchItem(firstProduct.productId, 4),
        InventoryMovementBatchItem(secondProduct.productId, 2)
    )) = InventoryMovementBatchRequest(wallet.walletId, MovementType.ENTRADA, "Compra", items)
}
