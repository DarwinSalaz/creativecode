package com.portafolio.mappers

import com.fasterxml.jackson.annotation.JsonProperty
import com.portafolio.dtos.ApplicationUserCreateDto
import com.portafolio.dtos.ExpenseDto
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.RelUserWallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import javax.persistence.Column

@Component
class ApplicationUserMapper {

    @Autowired
    private lateinit var bCryptPasswordEncoder: BCryptPasswordEncoder

    fun map(applicationUserCreateDto: ApplicationUserCreateDto) =
        ApplicationUser(
            companyId = applicationUserCreateDto.companyId,
            username = applicationUserCreateDto.username,
            name = applicationUserCreateDto.name,
            lastName = applicationUserCreateDto.lastName,
            cellphone = applicationUserCreateDto.cellphone,
            email = applicationUserCreateDto.email,
            password = applicationUserCreateDto.password,
            userProfileId = applicationUserCreateDto.userProfileId,
            active = true
        )

    fun mapRevert(applicationUser: ApplicationUser, walletIds: List<Int>?) =
        ApplicationUserCreateDto(
            applicationUserId = applicationUser.applicationUserId,
            companyId = applicationUser.companyId,
            username = applicationUser.username,
            name = applicationUser.name,
            lastName = applicationUser.lastName,
            cellphone = applicationUser.cellphone,
            email = applicationUser.email,
            password = applicationUser.password,
            userProfileId = applicationUser.userProfileId,
            walletIds = walletIds
        )

    fun map(applicationUserCreateDto: ApplicationUserCreateDto, applicationUser: ApplicationUser) : ApplicationUser {
        applicationUser.companyId = applicationUserCreateDto.companyId
        applicationUser.username = applicationUserCreateDto.username
        applicationUser.name = applicationUserCreateDto.name
        applicationUser.lastName = applicationUserCreateDto.lastName
        applicationUser.cellphone = applicationUserCreateDto.cellphone
        applicationUser.email = applicationUserCreateDto.email
        if (applicationUserCreateDto.password !== "" && applicationUserCreateDto.password !== "no_change") {
            applicationUser.password = bCryptPasswordEncoder.encode(applicationUserCreateDto.password)
        }
        //applicationUser.userProfileId = applicationUserCreateDto.userProfileId
        applicationUser.active = true

        return applicationUser
    }

    fun map(applicationUserId: Long, walletIds: List<Int>) : List<RelUserWallet>{

        return walletIds.map { RelUserWallet(applicationUserId = applicationUserId, walletId = it) }
    }


}