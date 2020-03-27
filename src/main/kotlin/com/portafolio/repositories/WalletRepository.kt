package com.portafolio.repositories

import com.portafolio.entities.Product
import com.portafolio.entities.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository: JpaRepository<Wallet, Int>