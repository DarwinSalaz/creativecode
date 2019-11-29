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
    val name: String,

    @JsonProperty("last_name")
    @Column(name = "last_name")
    val lastName: String? = null,

    @Column(name = "cellphone")
    val cellphone: String? = null,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "address")
    val address: String? = null,

    @JsonProperty("identification_number")
    @Column(name = "identification_number")
    val identificationNumber: String? = null,

    @Column(name = "active")
    val active: Boolean = true,

    @JsonProperty("created_at")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),

    @JsonProperty("gender")
    val gender: String? = "m",

    @JsonProperty("icon")
    val icon: String? = "av-1.png"

)