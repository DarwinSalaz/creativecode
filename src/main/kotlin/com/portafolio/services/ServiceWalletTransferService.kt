package com.portafolio.services

import com.portafolio.dtos.ServiceWalletTransferPreview
import com.portafolio.dtos.ServiceWalletTransferRequest
import com.portafolio.dtos.ServiceWalletTransferResponse
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.Customer
import com.portafolio.entities.ServiceWalletTransfer
import com.portafolio.repositories.CashControlRepository
import com.portafolio.repositories.CashMovementRepository
import com.portafolio.repositories.CustomerRepository
import com.portafolio.repositories.PaymentRepository
import com.portafolio.repositories.ServiceRepository
import com.portafolio.repositories.ServiceProductRepository
import com.portafolio.repositories.ServiceWalletTransferRepository
import com.portafolio.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ServiceWalletTransferService(
    private val serviceRepository: ServiceRepository,
    private val walletRepository: WalletRepository,
    private val customerRepository: CustomerRepository,
    private val paymentRepository: PaymentRepository,
    private val cashMovementRepository: CashMovementRepository,
    private val cashControlRepository: CashControlRepository,
    private val serviceProductRepository: ServiceProductRepository,
    private val transferRepository: ServiceWalletTransferRepository
) {
    companion object {
        const val MOVE_CUSTOMER = "MOVE_CUSTOMER"
        const val REUSE_TARGET_CUSTOMER = "REUSE_TARGET_CUSTOMER"
        const val COPY_CUSTOMER = "COPY_CUSTOMER"
    }

    fun preview(request: ServiceWalletTransferRequest, administrator: ApplicationUser): ServiceWalletTransferPreview {
        validateAdministrator(administrator)
        val context = loadContext(request, administrator)
        val movements = cashMovementRepository.findCashMovementsByServiceId(context.service.serviceId)
        val cashControlId = movements.firstOrNull { it.cashControlId != null }?.cashControlId
        val cashControlClosed = cashControlId?.let {
            cashControlRepository.findById(it).map { cashControl -> !cashControl.active }.orElse(false)
        } ?: false
        val inventoryWalletIds = serviceProductRepository
            .findProductWalletIdsByServiceId(context.service.serviceId)
        val inventoryMatchesTargetWallet = inventoryWalletIds.isNotEmpty() && inventoryWalletIds.all {
            it == context.targetWallet.walletId
        }

        val warnings = mutableListOf(
            "La venta y sus movimientos financieros cambiarán de cartera.",
            "La caja original y sus valores liquidados no serán modificados.",
            "Los movimientos históricos de inventario conservarán la cartera desde la que salió el producto."
        )
        if (cashControlClosed) {
            warnings.add("La venta pertenece a una caja cerrada y continuará asociada a esa caja.")
        }

        return ServiceWalletTransferPreview(
            serviceId = context.service.serviceId,
            sourceWalletId = context.sourceWallet.walletId,
            sourceWalletName = context.sourceWallet.name,
            targetWalletId = context.targetWallet.walletId,
            targetWalletName = context.targetWallet.name,
            cashControlId = cashControlId,
            cashControlClosed = cashControlClosed,
            paymentsCount = paymentRepository.findAllPaymentsByServiceId(context.service.serviceId)?.size ?: 0,
            cashMovementsCount = movements.size,
            customerId = context.customer.customerId,
            targetCustomerId = context.targetCustomer?.customerId,
            customerOtherServicesCount = context.otherServicesCount,
            customerStrategy = context.customerStrategy,
            customerStrategyMessage = customerStrategyMessage(context),
            inventoryWalletIds = inventoryWalletIds,
            inventoryMatchesTargetWallet = inventoryMatchesTargetWallet,
            inventoryWarning = inventoryWarning(inventoryWalletIds, context.targetWallet.walletId),
            warnings = warnings
        )
    }

    @Transactional
    fun transfer(request: ServiceWalletTransferRequest, administrator: ApplicationUser): ServiceWalletTransferResponse {
        validateAdministrator(administrator)
        val justification = request.justification?.trim()
        if (justification.isNullOrEmpty()) {
            throw IllegalArgumentException("La justificación del traslado es obligatoria.")
        }

        val context = loadContext(request, administrator)
        val targetCustomer = resolveTargetCustomer(context)
        val movements = cashMovementRepository.findCashMovementsByServiceId(context.service.serviceId)
        val cashControlId = movements.firstOrNull { it.cashControlId != null }?.cashControlId

        val updated = serviceRepository.transferToWallet(
            context.service.serviceId,
            context.targetWallet.walletId,
            targetCustomer.customerId
        )
        if (updated != 1) {
            throw IllegalStateException("No fue posible actualizar la venta.")
        }

        movements.forEach { it.walletId = context.targetWallet.walletId }
        cashMovementRepository.saveAll(movements)

        val transfer = transferRepository.save(ServiceWalletTransfer(
            serviceId = context.service.serviceId,
            sourceWalletId = context.sourceWallet.walletId,
            targetWalletId = context.targetWallet.walletId,
            sourceCustomerId = context.customer.customerId,
            targetCustomerId = targetCustomer.customerId,
            customerStrategy = context.customerStrategy,
            cashControlId = cashControlId,
            transferredBy = administrator.username,
            justification = justification
        ))

        return ServiceWalletTransferResponse(
            transferId = transfer.serviceWalletTransferId,
            serviceId = transfer.serviceId,
            sourceWalletId = transfer.sourceWalletId,
            targetWalletId = transfer.targetWalletId,
            sourceCustomerId = transfer.sourceCustomerId,
            targetCustomerId = transfer.targetCustomerId,
            customerStrategy = transfer.customerStrategy,
            cashControlId = transfer.cashControlId,
            transferredAt = transfer.createdAt
        )
    }

    private fun loadContext(request: ServiceWalletTransferRequest, administrator: ApplicationUser): TransferContext {
        val service = serviceRepository.findById(request.serviceId)
            .orElseThrow { IllegalArgumentException("La venta no existe.") }
        if (service.deletedAt != null || service.state == "deleted" || service.state == "canceled") {
            throw IllegalArgumentException("No se puede trasladar una venta eliminada o cancelada.")
        }
        if (service.walletId == request.targetWalletId) {
            throw IllegalArgumentException("La venta ya pertenece a la cartera seleccionada.")
        }

        val sourceWallet = walletRepository.findById(service.walletId)
            .orElseThrow { IllegalArgumentException("La cartera origen no existe.") }
        val targetWallet = walletRepository.findById(request.targetWalletId)
            .orElseThrow { IllegalArgumentException("La cartera destino no existe.") }
        if (!targetWallet.active) {
            throw IllegalArgumentException("La cartera destino está inactiva.")
        }
        if (sourceWallet.company.companyId != targetWallet.company.companyId ||
            administrator.companyId != sourceWallet.company.companyId) {
            throw IllegalArgumentException("Las carteras deben pertenecer a la misma empresa del administrador.")
        }

        val customer = customerRepository.findById(service.customerId)
            .orElseThrow { IllegalArgumentException("El cliente asociado no existe.") }
        val targetCustomer = customer.identificationNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { customerRepository.findCustomerByIdAndWallet(it, targetWallet.walletId)?.firstOrNull() }
        val otherServicesCount = serviceRepository.countOtherServicesByCustomer(customer.customerId, service.serviceId)
        val strategy = when {
            targetCustomer != null -> REUSE_TARGET_CUSTOMER
            otherServicesCount == 0L -> MOVE_CUSTOMER
            else -> COPY_CUSTOMER
        }

        return TransferContext(
            service = service,
            sourceWallet = sourceWallet,
            targetWallet = targetWallet,
            customer = customer,
            targetCustomer = targetCustomer,
            otherServicesCount = otherServicesCount,
            customerStrategy = strategy
        )
    }

    private fun resolveTargetCustomer(context: TransferContext): Customer {
        return when (context.customerStrategy) {
            REUSE_TARGET_CUSTOMER -> context.targetCustomer!!
            MOVE_CUSTOMER -> {
                context.customer.walletId = context.targetWallet.walletId
                customerRepository.save(context.customer)
            }
            COPY_CUSTOMER -> customerRepository.save(context.customer.copy(
                customerId = 0,
                walletId = context.targetWallet.walletId,
                createdAt = LocalDateTime.now()
            ))
            else -> throw IllegalStateException("Estrategia de cliente no soportada.")
        }
    }

    private fun customerStrategyMessage(context: TransferContext): String {
        return when (context.customerStrategy) {
            REUSE_TARGET_CUSTOMER -> "El cliente ya existe en la cartera destino y la venta se asociará a ese registro."
            MOVE_CUSTOMER -> "El cliente no tiene otras ventas asociadas y también será movido a la cartera destino."
            COPY_CUSTOMER -> "El cliente tiene otras ventas en la cartera origen; se creará una copia en la cartera destino."
            else -> ""
        }
    }

    private fun inventoryWarning(inventoryWalletIds: List<Int>, targetWalletId: Int): String {
        if (inventoryWalletIds.isEmpty()) {
            return "La venta no tiene productos activos asociados."
        }
        if (inventoryWalletIds.all { it == targetWalletId }) {
            return "Los productos de la venta pertenecen a la cartera destino. El inventario histórico no se modificará."
        }
        return "Los productos salieron de las carteras ${inventoryWalletIds.joinToString()}. " +
            "El traslado no moverá ni recalculará ese inventario histórico."
    }

    private fun validateAdministrator(user: ApplicationUser) {
        if (user.userProfileId != 1) {
            throw SecurityException("Solo un administrador puede trasladar ventas entre carteras.")
        }
    }

    private data class TransferContext(
        val service: com.portafolio.entities.Service,
        val sourceWallet: com.portafolio.entities.Wallet,
        val targetWallet: com.portafolio.entities.Wallet,
        val customer: Customer,
        val targetCustomer: Customer?,
        val otherServicesCount: Long,
        val customerStrategy: String
    )
}
