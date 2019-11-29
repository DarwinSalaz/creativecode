package com.portafolio.repositories

import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
interface ApplicationUserRepository: JpaRepository <ApplicationUser, Long> {

    fun findByUsername(username: String) : ApplicationUser?

}