package com.portafolio.entities

import java.io.Serializable
import javax.persistence.Table
import javax.persistence.Entity
import javax.persistence.IdClass
import javax.persistence.Id
import javax.persistence.Column

@Table(name = "rel_user_wallets")
@Entity
@IdClass(RelUserWalletPrimaryKey::class)
data class RelUserWallet (

    @Id
    @Column(name = "wallet_id", insertable = false, updatable = false)
    val walletId: Int,

    @Id
    @Column(name = "application_user_id", insertable = false, updatable = false)
    var applicationUserId: Long

)

data class RelUserWalletPrimaryKey(

    val walletId: Int = 0,

    val applicationUserId: Long = 0

) : Serializable