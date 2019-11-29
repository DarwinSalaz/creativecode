package com.portafolio.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import javax.persistence.*

@Table(name = "application_users")
@Entity
data class ApplicationUser (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("application_user_id")
    @Column(name = "application_user_id")
    val applicationUserId: Long,

    @Column(name = "company_id")
    @JsonProperty("company_id")
    val companyId: Int,

    @Column(name = "username")
    val username: String,

    @Column(name = "name")
    val name: String,

    @JsonProperty("last_name")
    @Column(name = "last_name")
    val lastName: String? = null,

    @Column(name = "password")
    var password: String,

    @Column(name = "user_profile_id")
    @JsonProperty("user_profile_id")
    val userProfileId: Int,

    @Column(name = "created_at")
    @JsonProperty("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

)