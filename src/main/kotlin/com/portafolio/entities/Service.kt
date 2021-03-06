package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.*

@Table(name = "services")
@Entity
data class Service (
    @Id
    @JsonProperty("service_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    val serviceId: Long = 0,

    @Column(name = "application_user_id")
    val applicationUserId: Long = 0,

    @Column(name = "service_value")
    val serviceValue: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "down_payment")
    val downPayment: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "discount")
    val discount: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "total_value")
    val totalValue: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "debt")
    val debt: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "days_per_fee")
    val daysPerFee: Int = 0,

    @Column(name = "quantity_of_fees")
    val quantityOfFees: Int = 0,

    @Column(name = "fee_value")
    val feeValue: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "wallet_id")
    val walletId: Int = 0,

    @Column(name = "has_products")
    val hasProducts: Boolean = false,

    @Column(name = "customer_id")
    val customerId: Long = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    val state: String = "",

    @Column(name = "observations")
    val observations: String? = null,

    @Column(name = "next_payment_date")
    val nextPaymentDate: LocalDateTime? = null

) {
    @JsonProperty("service_products")
    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var serviceProducts: MutableSet<ServiceProduct> = mutableSetOf()
}