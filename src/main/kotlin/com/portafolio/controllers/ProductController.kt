package com.portafolio.controllers

import com.portafolio.dtos.*
import com.portafolio.entities.Product
import com.portafolio.mappers.ProductMapper
import com.portafolio.models.InventoryDetail
import com.portafolio.repositories.ProductRepository
import com.portafolio.repositories.WalletGroupRepository
import com.portafolio.services.ServiceProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.temporal.ChronoUnit
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
class ProductController {

    @Autowired
    lateinit var repository: ProductRepository

    @Autowired
    lateinit var walletGroupRepository: WalletGroupRepository

    @Autowired
    lateinit var serviceProductService: ServiceProductService

    @Autowired
    lateinit var mapper: ProductMapper

    @PostMapping("/products")
    fun getProducts(@PageableDefault(size = 1000) pageable: Pageable, @Valid @RequestBody walletRequest: WalletRequest?) : ResponseEntity<ProductResponse> {
        var includeWalletName = false
        val products = if (walletRequest == null || walletRequest.walletIds.isNullOrEmpty()) {
            //repository.findAll(pageable).content
            repository.findAll()
        } else {
            var walletIds = walletRequest.walletIds
            includeWalletName = walletIds.size > 1

            if (walletIds.size == 1) {
                val walletGroups = walletGroupRepository.findAllWalletsGroup(walletIds.first())
                walletGroups?.let { wg ->
                    if (wg.size > 1) {
                        walletIds = wg.map { it.walletId }
                    }
                }
            }

            repository.findAllProductByWallets(walletIds!!, pageable)
        }

        return ResponseEntity.ok().body(ProductResponse(products = mapper.map(products, includeWalletName)));
    }

    @PostMapping("/product/create")
    fun createProduct(@Valid @RequestBody productDto: ProductDto) : Product {

        return repository.save(mapper.mapRev(productDto))
    }

    @PutMapping("/product/{id_product}")
    fun update(@Valid @RequestBody productDto: ProductDto, @PathVariable("id_product") idProduct : Long) : Product? {
        var product = repository.findById(idProduct).get()

        product = mapper.mapProductUpdate(product, productDto)
        return repository.save(product)
    }

    @PostMapping("inventory/report")
    fun inventoryReport(@Valid @RequestBody request: ResumeWalletRequest): List<InventoryDetail> {

        return serviceProductService.getInventoryReport(request.walletId, request.startsAt.truncatedTo(ChronoUnit.DAYS), request.endsAt.withHour(23).withMinute(59).withSecond(59))
    }

}