package com.portafolio.repositories

import com.portafolio.entities.WalletGroup
import com.portafolio.entities.WalletGroupPrimaryKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WalletGroupRepository: JpaRepository<WalletGroup, WalletGroupPrimaryKey> {

    @Query("SELECT wgs FROM WalletGroup wg INNER JOIN WalletGroup wgs ON (wg.walletGroupId = wgs.walletGroupId) WHERE wg.walletId = ?1")
    fun findAllWalletsGroup(walletId : Int) : List<WalletGroup>?

}