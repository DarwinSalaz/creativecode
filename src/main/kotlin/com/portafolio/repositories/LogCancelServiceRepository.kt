package com.portafolio.repositories

import com.portafolio.entities.LogCancelService
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LogCancelServiceRepository: JpaRepository<LogCancelService, Long> {}