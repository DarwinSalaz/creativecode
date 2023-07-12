package com.portafolio.entities

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
    val applicationUserId: Long = 0,

    @Column(name = "company_id")
    @JsonProperty("company_id")
    var companyId: Int,

    @Column(name = "username")
    var username: String,

    @Column(name = "name")
    var name: String,

    @JsonProperty("last_name")
    @Column(name = "last_name")
    var lastName: String? = null,

    @JsonProperty("cellphone")
    @Column(name = "cellphone")
    var cellphone: String? = null,

    @JsonProperty("email")
    @Column(name = "email")
    var email: String? = null,

    @Column(name = "password")
    var password: String,

    @Column(name = "user_profile_id")
    @JsonProperty("user_profile_id")
    val userProfileId: Int,

    @Column(name = "created_at")
    @JsonProperty("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "active")
    @JsonProperty("active")
    var active: Boolean

) {

    fun isSalesman() = userProfileId in listOf(1, 3)

}