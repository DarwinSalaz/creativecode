package com.portafolio.mappers

import com.portafolio.dtos.ProductDto
import com.portafolio.entities.Company
import com.portafolio.entities.Product
import com.portafolio.repositories.CompanyRepository
import com.portafolio.repositories.WalletRepository
import com.portafolio.services.Utilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import kotlin.math.cos

@Component
class ProductMapper {

    @Autowired
    lateinit var utilities: Utilities

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var walletRepository: WalletRepository

    fun map(products: List<Product>, includeWalletName: Boolean = false) =
        products.map { product ->
            ProductDto(
                companyId = product.company.companyId,
                productId = product.productId,
                name = if (includeWalletName) product.name + " - " + product.wallet.name else product.name,
                description = product.description,
                value = product.value,
                cost = product.cost,
                leftQuantity = product.leftQuantity,
                valueStr = utilities.currencyFormat(product.value.toString()),
                walletId = product.wallet.walletId,
                value4Installments = product.value4Installments,
                value6Installments = product.value6Installments,
                value8Installments = product.value8Installments,
                value10Installments = product.value10Installments
            )
        }

    fun mapRev(productDto: ProductDto) =
        Product(
            company = companyRepository.findById(productDto.companyId!!).get(),
            name = productDto.name,
            description = productDto.description,
            value = productDto.value,
            cost = productDto.cost ?: BigDecimal.ZERO,
            wallet = walletRepository.findById(productDto.walletId).get(),
            leftQuantity = productDto.leftQuantity,
            value4Installments = productDto.value4Installments,
            value6Installments = productDto.value6Installments,
            value8Installments = productDto.value8Installments,
            value10Installments = productDto.value10Installments
        )

    fun mapProductUpdate(product: Product, productDto: ProductDto) : Product {
        product.name = productDto.name
        product.description = productDto.description
        product.value = productDto.value
        product.cost = productDto.cost ?: BigDecimal.ZERO
        product.leftQuantity = productDto.leftQuantity
        product.value4Installments = productDto.value4Installments
        product.value6Installments = productDto.value6Installments
        product.value8Installments = productDto.value8Installments
        product.value10Installments = productDto.value10Installments

        return product
    }

}
