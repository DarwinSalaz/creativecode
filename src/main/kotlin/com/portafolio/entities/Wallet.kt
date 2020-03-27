package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Column
import javax.persistence.ManyToOne
import javax.persistence.FetchType
import javax.persistence.CascadeType
import javax.persistence.JoinColumn
import javax.persistence.Table
import javax.persistence.Entity

@Table(name = "wallets")
@Entity
data class Wallet (

    @Id
    @JsonProperty("wallet_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    val walletId: Int = 0,

    @ManyToOne(fetch= FetchType.LAZY, cascade= [CascadeType.PERSIST])
    @JoinColumn(name = "company_id")
    var company: Company,

    @Column(name = "name")
    var name: String,

    @Column(name = "active")
    var active: Boolean

)