package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.persistence.*

@Table(name = "customers")
@Entity
data class Customer (
    @Id
    @JsonProperty("customer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    val customerId: Long = 0,

    @ManyToOne(fetch= FetchType.LAZY, cascade= [CascadeType.PERSIST])
    @JoinColumn(name = "company_id")
    var company: Company,

    @NotNull
    @Column(name = "name")
    var name: String,

    @JsonProperty("last_name")
    @Column(name = "last_name")
    var lastName: String? = null,

    @Column(name = "cellphone")
    var cellphone: String? = null,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @JsonProperty("identification_number")
    @Column(name = "identification_number")
    var identificationNumber: String? = null,

    @Column(name = "active")
    var active: Boolean = true,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),

    @JsonProperty("gender")
    var gender: String? = "m",

    @JsonProperty("icon")
    val icon: String? = "av-1.png",

    @JsonProperty("observation")
    @Column(name = "observation")
    var observation: String? = null,

    @JsonProperty("wallet_id")
    @Column(name = "wallet_id")
    var walletId: Int,

    @Column(name = "blocked")
    var blocked: Boolean = false

)