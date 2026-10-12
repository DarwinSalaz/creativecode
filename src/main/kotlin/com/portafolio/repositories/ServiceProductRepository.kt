package com.portafolio.repositories

import com.portafolio.entities.ServiceProduct
import com.portafolio.entities.ServiceProductPrimaryKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ServiceProductRepository: JpaRepository<ServiceProduct, ServiceProductPrimaryKey> {

    @Query("SELECT sp FROM ServiceProduct sp WHERE sp.service.walletId = ?1 and sp.service.createdAt between ?2 and ?3")
    fun findServiceProducts(walletId: Int, startsAt: LocalDateTime, endsAt: LocalDateTime) : List<ServiceProduct>

    @Query(value = """
        SELECT DISTINCT p.wallet_id
        FROM service_products sp
        INNER JOIN products p ON p.product_id = sp.product_id
        WHERE sp.service_id = ?1
          AND (sp.enabled IS NULL OR sp.enabled = TRUE)
        ORDER BY p.wallet_id
    """, nativeQuery = true)
    fun findProductWalletIdsByServiceId(serviceId: Long): List<Int>

}
