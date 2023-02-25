package com.portafolio.mappers

import com.fasterxml.jackson.annotation.JsonProperty
import com.portafolio.dtos.ApplicationUserCreateDto
import com.portafolio.dtos.ExpenseDto
import com.portafolio.entities.ApplicationUser
import com.portafolio.entities.RelUserWallet
import org.springframework.stereotype.Component
import javax.persistence.Column

@Component
class ApplicationUserMapper {

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

    fun map(applicationUserId: Long, walletIds: List<Int>) : List<RelUserWallet>{

        return walletIds.map { RelUserWallet(applicationUserId = applicationUserId, walletId = it) }
    }


}