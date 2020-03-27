package com.portafolio.repositories

import com.portafolio.entities.Company
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class CompanyRepository(val entityManager: EntityManager) : SimpleJpaRepository<Company, Int>(Company::class.java, entityManager), JpaSpecificationExecutor<Company>