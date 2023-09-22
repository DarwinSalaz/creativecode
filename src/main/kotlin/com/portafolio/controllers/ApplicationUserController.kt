package com.portafolio.controllers

import com.portafolio.dtos.ApplicationUserCreateDto
import com.portafolio.dtos.CustomerDto
import com.portafolio.entities.ApplicationUser
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
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT])
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

    @PutMapping("/application_user/{application_user_id}")
    fun updateApplicationUser(@Valid @RequestBody applicationUserCreateDto: ApplicationUserCreateDto, @PathVariable("application_user_id") applicationUserId : Long) : ApplicationUser {
        var applicationUser = repository.getOne(applicationUserId)
        applicationUser = mapper.map(applicationUserCreateDto, applicationUser)

        return repository.save(applicationUser)
    }

    @GetMapping("/application_user/{username}")
    fun getApplicationUserByUsername(@PathVariable("username") username : String) : ApplicationUserCreateDto? {
        val applicationUser = repository.findByUsername(username)
        applicationUser?.let {
            val wallets = relUserWalletRepository.findAllWalletsByUser(applicationUserId = applicationUser.applicationUserId)

            return mapper.mapRevert(applicationUser, wallets?.map { it.walletId })
        } ?: return null
    }

    @PutMapping("/application_user/inactivate")
    fun inactivate(@RequestParam("username") username: String) : ResponseEntity<HashMap<String, Boolean>> {
        val user = repository.findByUsername(username) ?: return ResponseEntity.ok().body(hashMapOf("ok" to false))
        user.active = false
        repository.save(user)

        return ResponseEntity.ok().body(hashMapOf("ok" to true))
    }

    @GetMapping("/application_user/login")
    fun logIn(@RequestHeader(required = true) username: String,
              @RequestHeader(required = true) password: String) : HashMap<String, Any?> {
        var response : HashMap<String, Any?>
        try {
            val token = service.logIn(password, username)

            val user = repository.findByUsername(username)

            response = if (user?.active == false) {
                hashMapOf("ok" to false, "token" to null)
            } else {
                val userProfileId = user!!.userProfileId
                val relUserWallets = relUserWalletRepository.findAllWalletsByUser(user.applicationUserId)
                val walletIds = relUserWallets?.map { it.walletId } ?: listOf()

                hashMapOf("ok" to !token.isNullOrEmpty(), "token" to token, "user_profile_id" to userProfileId, "wallet_ids" to walletIds)
            }
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