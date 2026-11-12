package com.portafolio.repositories

import com.portafolio.entities.RelUserWallet
import com.portafolio.entities.RelUserWalletPrimaryKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RelUserWalletRepository : JpaRepository<RelUserWallet, RelUserWalletPrimaryKey> {

    @Query("SELECT r FROM RelUserWallet r WHERE r.applicationUserId = ?1")
    fun findAllWalletsByUser(applicationUserId : Long) : List<RelUserWallet>?

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RelUserWallet r WHERE r.applicationUserId = ?1")
    fun deleteAllWalletsByUser(applicationUserId: Long): Int

}
