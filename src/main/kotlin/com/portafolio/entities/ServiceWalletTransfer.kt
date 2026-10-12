package com.portafolio.entities

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "service_wallet_transfers")
@Entity
data class ServiceWalletTransfer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_wallet_transfer_id")
    val serviceWalletTransferId: Long = 0,

    @Column(name = "service_id", nullable = false)
    val serviceId: Long,

    @Column(name = "source_wallet_id", nullable = false)
    val sourceWalletId: Int,

    @Column(name = "target_wallet_id", nullable = false)
    val targetWalletId: Int,

    @Column(name = "source_customer_id", nullable = false)
    val sourceCustomerId: Long,

    @Column(name = "target_customer_id", nullable = false)
    val targetCustomerId: Long,

    @Column(name = "customer_strategy", nullable = false)
    val customerStrategy: String,

    @Column(name = "cash_control_id")
    val cashControlId: Long?,

    @Column(name = "transferred_by", nullable = false)
    val transferredBy: String,

    @Column(name = "justification", nullable = false)
    val justification: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
