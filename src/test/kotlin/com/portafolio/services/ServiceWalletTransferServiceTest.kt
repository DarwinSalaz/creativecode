package com.portafolio.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.portafolio.dtos.ServiceWalletTransferRequest
import com.portafolio.entities.*
import com.portafolio.repositories.*
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Optional

class ServiceWalletTransferServiceTest {
    private val serviceRepository: ServiceRepository = mock()
    private val walletRepository: WalletRepository = mock()
    private val customerRepository: CustomerRepository = mock()
    private val paymentRepository: PaymentRepository = mock()
    private val cashMovementRepository: CashMovementRepository = mock()
    private val cashControlRepository: CashControlRepository = mock()
    private val serviceProductRepository: ServiceProductRepository = mock()
    private val transferRepository: ServiceWalletTransferRepository = mock()
    private lateinit var transferService: ServiceWalletTransferService
    private lateinit var company: Company
    private lateinit var sourceWallet: Wallet
    private lateinit var targetWallet: Wallet
    private lateinit var customer: Customer
    private lateinit var sale: com.portafolio.entities.Service
    private lateinit var administrator: ApplicationUser

    @Before
    fun setUp() {
        transferService = ServiceWalletTransferService(serviceRepository, walletRepository, customerRepository,
            paymentRepository, cashMovementRepository, cashControlRepository, serviceProductRepository,
            transferRepository)
        company = Company(1, "Empresa", "123", true)
        sourceWallet = Wallet(10, company, "Origen", true)
        targetWallet = Wallet(20, company, "Destino", true)
        customer = Customer(customerId = 30, company = company, name = "Ana",
            identificationNumber = "12345", walletId = sourceWallet.walletId)
        sale = com.portafolio.entities.Service(serviceId = 40, applicationUserId = 50,
            walletId = sourceWallet.walletId, customerId = customer.customerId, state = "created")
        administrator = ApplicationUser(applicationUserId = 1, companyId = company.companyId,
            username = "admin", name = "Admin", password = "secret", userProfileId = 1, active = true)

        whenever(serviceRepository.findById(sale.serviceId)).thenReturn(Optional.of(sale))
        whenever(walletRepository.findById(sourceWallet.walletId)).thenReturn(Optional.of(sourceWallet))
        whenever(walletRepository.findById(targetWallet.walletId)).thenReturn(Optional.of(targetWallet))
        whenever(customerRepository.findById(customer.customerId)).thenReturn(Optional.of(customer))
        whenever(customerRepository.findCustomerByIdAndWallet(customer.identificationNumber!!, targetWallet.walletId))
            .thenReturn(emptyList())
        whenever(paymentRepository.findAllPaymentsByServiceId(sale.serviceId)).thenReturn(emptyList())
        whenever(cashMovementRepository.findCashMovementsByServiceId(sale.serviceId)).thenReturn(emptyList())
        whenever(serviceProductRepository.findProductWalletIdsByServiceId(sale.serviceId))
            .thenReturn(listOf(sourceWallet.walletId))
    }

    @Test
    fun `rejects transfers requested by non administrators`() {
        val seller = administrator.copy(userProfileId = 2)
        try {
            transferService.preview(request(), seller)
            fail("Expected SecurityException")
        } catch (error: SecurityException) {
            assertEquals("Solo un administrador puede trasladar ventas entre carteras.", error.message)
        }
    }

    @Test
    fun `moves the original customer when it has no other sales`() {
        whenever(serviceRepository.countOtherServicesByCustomer(customer.customerId, sale.serviceId)).thenReturn(0)
        val preview = transferService.preview(request(), administrator)
        assertEquals(ServiceWalletTransferService.MOVE_CUSTOMER, preview.customerStrategy)
        assertEquals(0L, preview.customerOtherServicesCount)
        assertEquals(listOf(sourceWallet.walletId), preview.inventoryWalletIds)
        assertEquals(false, preview.inventoryMatchesTargetWallet)
    }

    @Test
    fun `reuses a customer that already exists in target wallet`() {
        val targetCustomer = customer.copy(customerId = 31, walletId = targetWallet.walletId)
        whenever(customerRepository.findCustomerByIdAndWallet(customer.identificationNumber!!, targetWallet.walletId))
            .thenReturn(listOf(targetCustomer))
        whenever(serviceRepository.countOtherServicesByCustomer(customer.customerId, sale.serviceId)).thenReturn(2)
        val preview = transferService.preview(request(), administrator)
        assertEquals(ServiceWalletTransferService.REUSE_TARGET_CUSTOMER, preview.customerStrategy)
        assertEquals(targetCustomer.customerId, preview.targetCustomerId)
    }

    @Test
    fun `does not report matching inventory when sale has no active products`() {
        whenever(serviceRepository.countOtherServicesByCustomer(customer.customerId, sale.serviceId)).thenReturn(0)
        whenever(serviceProductRepository.findProductWalletIdsByServiceId(sale.serviceId)).thenReturn(emptyList())

        val preview = transferService.preview(request(), administrator)

        assertEquals(emptyList<Int>(), preview.inventoryWalletIds)
        assertEquals(false, preview.inventoryMatchesTargetWallet)
        assertEquals("La venta no tiene productos activos asociados.", preview.inventoryWarning)
    }

    @Test
    fun `copies customer and reclassifies movements when origin customer has other sales`() {
        val movement = CashMovement(cashMovementType = "new_service", movementType = "IN",
            applicationUserId = sale.applicationUserId, paymentId = null, serviceId = sale.serviceId,
            value = BigDecimal.ZERO, description = "Ana", cashControlId = 80,
            commission = BigDecimal.ZERO, walletId = sourceWallet.walletId, expenseId = null, revenueId = null)
        whenever(serviceRepository.countOtherServicesByCustomer(customer.customerId, sale.serviceId)).thenReturn(1)
        whenever(customerRepository.save(any<Customer>())).thenAnswer {
            (it.arguments[0] as Customer).copy(customerId = 31)
        }
        whenever(serviceRepository.transferToWallet(sale.serviceId, targetWallet.walletId, 31)).thenReturn(1)
        whenever(cashMovementRepository.findCashMovementsByServiceId(sale.serviceId)).thenReturn(listOf(movement))
        whenever(transferRepository.save(any<ServiceWalletTransfer>())).thenAnswer {
            (it.arguments[0] as ServiceWalletTransfer).copy(serviceWalletTransferId = 90)
        }

        val response = transferService.transfer(request("Corrección de cartera"), administrator)

        assertEquals(ServiceWalletTransferService.COPY_CUSTOMER, response.customerStrategy)
        assertEquals(31L, response.targetCustomerId)
        assertEquals(targetWallet.walletId, movement.walletId)
        assertEquals(80L, response.cashControlId)
        verify(serviceRepository).transferToWallet(sale.serviceId, targetWallet.walletId, 31)
        verify(cashMovementRepository).saveAll(eq(listOf(movement)))
    }

    private fun request(justification: String? = null) = ServiceWalletTransferRequest(
        serviceId = sale.serviceId, targetWalletId = targetWallet.walletId, justification = justification)
}
