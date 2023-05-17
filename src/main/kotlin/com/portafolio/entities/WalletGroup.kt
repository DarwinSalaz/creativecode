package com.portafolio.entities

import java.io.Serializable
import javax.persistence.*

@Table(name = "wallet_groups")
@Entity
@IdClass(WalletGroupPrimaryKey::class)
data class WalletGroup(

    @Column(name = "wallet_group_name")
    var walletGroupName: String,

    @Id
    @Column(name = "wallet_id", insertable = false, updatable = false)
    val walletId: Int,

    @Id
    @Column(name = "wallet_group_id", insertable = false, updatable = false)
    var walletGroupId: Int

)

data class WalletGroupPrimaryKey(

    val walletId: Int = 0,

    val walletGroupId: Int = 0

) : Serializable