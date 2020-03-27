package com.portafolio.services

import com.portafolio.entities.ApplicationUser
import com.portafolio.repositories.ApplicationUserRepository
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.HashSet
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.core.userdetails.User


@Service
class UserDetailsServiceImpl : UserDetailsService {
    @Autowired
    lateinit var userRepository: ApplicationUserRepository

    @Throws(UsernameNotFoundException::class)
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val appUser : ApplicationUser? = userRepository.findByUsername(username) ?: throw UsernameNotFoundException(username);

        val grantedAuthorities = HashSet<GrantedAuthority>()
        /*for (role in user.getRoles()) {
            grantedAuthorities.add(SimpleGrantedAuthority(role.getName()))
        }*/
        val user = User(appUser?.username, appUser?.password, grantedAuthorities) as UserDetails

        return user
    }
}
