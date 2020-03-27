package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.math.BigDecimal
import javax.persistence.*

@Table(name = "service_products")
@Entity
@IdClass(ServiceProductPrimaryKey::class)
data class ServiceProduct (

    @Id
    @Column(name = "product_id", insertable = false, updatable = false)
    @JsonProperty("product_id")
    val productId: Long,

    @Id
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serviceId", insertable = false, updatable = false)
    val service: Service,

    @Column(name = "value")
    val value: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "quantity")
    val quantity: Int = 0

)

data class ServiceProductPrimaryKey(

    val productId: Long = 0,

    val service: Service = Service()

) : Serializable
