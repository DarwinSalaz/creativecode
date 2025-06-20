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
    var downPayment: BigDecimal = BigDecimal.valueOf(0),

    // Nuevo campo: valor total que debería pagarse como seña (usualmente 10% del total sin descuento)
    @Column(name = "down_payment_total")
    var downPaymentTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount")
    var discount: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "total_value")
    var totalValue: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "debt")
    var debt: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "days_per_fee")
    val daysPerFee: Int = 0,

    @Column(name = "quantity_of_fees")
    var quantityOfFees: Int = 0,

    @Column(name = "fee_value")
    var feeValue: BigDecimal = BigDecimal.valueOf(0),

    @Column(name = "wallet_id")
    val walletId: Int = 0,

    @Column(name = "has_products")
    val hasProducts: Boolean = false,

    @Column(name = "customer_id")
    val customerId: Long = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var state: String = "",

    @Column(name = "observations")
    var observations: String? = null,

    @Column(name = "next_payment_date")
    var nextPaymentDate: LocalDateTime? = null,

    @Column(name = "pending_value")
    var pendingValue: BigDecimal? = null,

    @Column(name = "has_expired_payment")
    var hasExpiredPayment: Boolean? = false,

    @Column(name = "pending_fees")
    var pendingFees: Int? = 0,

    @Column(name = "marked_for_withdrawal")
    var markedForWithdrawal: Boolean = false,

    @Column(name = "marked_as_lost")
    var markedAsLost: Boolean = false,

    @Column(name = "pay_down_in_installments")
    val payDownInInstallments: Boolean = false

) {
    @JsonProperty("service_products")
    @OneToMany(mappedBy = "service", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var serviceProducts: MutableSet<ServiceProduct> = mutableSetOf()
}