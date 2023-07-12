package com.portafolio.services

import com.portafolio.models.InventoryDetail
import com.portafolio.repositories.ProductRepository
import com.portafolio.repositories.ServiceProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ServiceProductService {

    @Autowired
    private lateinit var repository: ServiceProductRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    fun getInventoryReport(walletId: Int, startAt: LocalDateTime, endsAt: LocalDateTime) : List<InventoryDetail> {
        val products = productRepository.findAllProductByWallets(listOf(walletId), Pageable.unpaged())
        val serviceProducts = repository.findServiceProducts(walletId, startAt, endsAt)

        val result = arrayListOf<InventoryDetail>()

        products.forEach { p ->
            val quantitySold = serviceProducts.filter { it.productId == p.productId }
                .map { it.quantity }.fold(0){ a, b -> a + b }

            val inventoryDetail = InventoryDetail(
                productId = p.productId,
                productName = p.name,
                quantitySold = quantitySold,
                leftQuantity = p.leftQuantity,
                walletName = p.wallet.name
            )

            result.add(inventoryDetail)
        }

        return result
    }
}