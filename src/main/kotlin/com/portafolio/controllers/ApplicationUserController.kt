package com.portafolio.controllers

import com.portafolio.dtos.ApplicationUserCreateDto
import com.portafolio.mappers.ApplicationUserMapper
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.repositories.RelUserWalletRepository
import com.portafolio.services.ApplicationUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ApplicationUserController {

    @Autowired
    private lateinit var service: ApplicationUserService

    @Autowired
    private lateinit var mapper: ApplicationUserMapper

    @Autowired
    private lateinit var repository: ApplicationUserRepository

    @Autowired
    private lateinit var relUserWalletRepository: RelUserWalletRepository

    @PostMapping("/application_user/create")
    fun createApplicationUser(@Valid @RequestBody applicationUserCreateDto: ApplicationUserCreateDto) : ResponseEntity<HashMap<String, Boolean>> {
        val userExist = service.existingUser(applicationUserCreateDto.username)

        if(userExist) return ResponseEntity.ok().body(hashMapOf("ok" to false))

        val user = mapper.map(applicationUserCreateDto)

        val userSaved = service.save(user)

        if(!applicationUserCreateDto.walletIds.isNullOrEmpty()) {
            val relWalletsUser = mapper.map(applicationUserId = userSaved.applicationUserId, walletIds = applicationUserCreateDto.walletIds)
            relUserWalletRepository.saveAll(relWalletsUser)
        }

        return ResponseEntity.ok().body(hashMapOf("ok" to true))
    }

    @GetMapping("/application_user/login")
    fun logIn(@RequestHeader(required = true) username: String,
              @RequestHeader(required = true) password: String) : HashMap<String, Any?> {
        var response : HashMap<String, Any?>
        try {
            val token = service.logIn(password, username)

            val user = repository.findByUsername(username)
            val userProfileId = user!!.userProfileId
            val relUserWallets = relUserWalletRepository.findAllWalletsByUser(user.applicationUserId)
            val walletIds = relUserWallets?.map { it.walletId } ?: listOf()

            response = hashMapOf("ok" to !token.isNullOrEmpty(), "token" to token, "user_profile_id" to userProfileId, "wallet_ids" to walletIds)
        } catch (e: UsernameNotFoundException) {
            response = hashMapOf("ok" to false, "token" to null)
        } catch (e: BadCredentialsException) {
            response = hashMapOf("ok" to false, "token" to null)
        } catch (e: Exception) {
            response = hashMapOf("ok" to false, "token" to null)
        }

        return response
    }

    @GetMapping("/application_user")
    fun getApplicationUsers() = repository.findAll()

}