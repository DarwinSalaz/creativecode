package com.portafolio.controllers

import com.portafolio.entities.ApplicationUser
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@CrossOrigin(origins = ["*"], methods= [RequestMethod.GET, RequestMethod.POST])
class ApplicationUserController {

    @Autowired
    private lateinit var service: ApplicationUserService

    @PostMapping("/application_user/create")
    fun createApplicationUser(@Valid @RequestBody applicationUser: ApplicationUser) : ResponseEntity<HashMap<String, Boolean>> {
        val userExist = service.existingUser(applicationUser.username)

        if(userExist) return ResponseEntity.ok().body(hashMapOf("ok" to false))

        service.save(applicationUser)

        return ResponseEntity.ok().body(hashMapOf("ok" to true))
    }

    @GetMapping("/application_user/login")
    fun logIn(@RequestHeader(required = true) username: String,
              @RequestHeader(required = true) password: String) : HashMap<String, Any?> {
        var response : HashMap<String, Any?>
        try {
            val token = service.logIn(password, username)
            response = hashMapOf("ok" to !token.isNullOrEmpty(), "token" to token)
        } catch (e: UsernameNotFoundException) {
            response = hashMapOf("ok" to false, "token" to null)
        } catch (e: BadCredentialsException) {
            response = hashMapOf("ok" to false, "token" to null)
        }

        return response
    }


}