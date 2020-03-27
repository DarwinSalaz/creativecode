package com.portafolio.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "companies")
@Entity
data class Company(

    @Id
    @Column(name = "company_id")
    val companyId: Int,

    @Column(name = "name")
    val name: String,

    @Column(name = "nit")
    val nit: String,

    @Column(name = "active")
    val active: Boolean
)