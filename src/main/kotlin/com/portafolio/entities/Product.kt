package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal
import javax.persistence.*

@Table(name = "products")
@Entity
data class Product (

    @Id
    @JsonProperty("product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    val productId: Long = 0,

    @ManyToOne(fetch= FetchType.LAZY, cascade= [CascadeType.PERSIST])
    @JoinColumn(name = "company_id")
    var company: Company,

    @NotNull
    @Column(name = "name")
    val name: String,

    @NotNull
    @Column(name = "description")
    val description: String,

    @NotNull
    @Column(name = "value")
    val value: BigDecimal

)