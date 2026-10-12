package com.portafolio.repositories

import com.portafolio.entities.ServiceWalletTransfer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceWalletTransferRepository : JpaRepository<ServiceWalletTransfer, Long>
