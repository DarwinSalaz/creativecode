package com.portafolio.repositories

import com.portafolio.entities.ServiceProduct
import com.portafolio.entities.ServiceProductPrimaryKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceProductRepository: JpaRepository<ServiceProduct, ServiceProductPrimaryKey> {}