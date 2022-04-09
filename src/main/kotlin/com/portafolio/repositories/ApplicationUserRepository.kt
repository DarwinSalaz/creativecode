package com.portafolio.repositories

import com.portafolio.entities.ApplicationUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationUserRepository: JpaRepository <ApplicationUser, Long> {

    fun findByUsername(username: String) : ApplicationUser?

}