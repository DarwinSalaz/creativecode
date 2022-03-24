package com.portafolio.mappers

import com.portafolio.dtos.ProductDto
import com.portafolio.entities.Product
import org.springframework.stereotype.Component

@Component
class ProductMapper {

    fun map(products: List<Product>, includeWalletName: Boolean = false) =
        products.map { product ->
            ProductDto(
                productId = product.productId,
                name = if (includeWalletName) product.name + " - " + product.wallet.name else product.name,
                description = product.description,
                value = product.value,
                walletId = product.wallet.walletId
            )
        }

}