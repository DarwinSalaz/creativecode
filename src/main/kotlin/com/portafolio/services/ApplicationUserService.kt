package com.portafolio.services

import com.portafolio.entities.ApplicationUser
import com.portafolio.repositories.ApplicationUserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Date
import java.util.stream.Collectors
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import javax.xml.bind.DatatypeConverter


@Service
class ApplicationUserService {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var repository: ApplicationUserRepository
    @Autowired
    private lateinit var bCryptPasswordEncoder: BCryptPasswordEncoder
    @Autowired
    private lateinit var userDetailsService: UserDetailsService
    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    fun save(user: ApplicationUser) : ApplicationUser {
        user.password = bCryptPasswordEncoder.encode(user.password)

        return repository.save(user)
    }

    fun existingUser(username: String) : Boolean {
        var response = true
        try {
            userDetailsService.loadUserByUsername(username)
        } catch (ex: UsernameNotFoundException) {
            response = false
        }

        return response
    }

    @Throws(UsernameNotFoundException::class)
    fun logIn(password : String, username : String) : String? {
        val userDetails = userDetailsService.loadUserByUsername(username)
        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)

        authenticationManager.authenticate(usernamePasswordAuthenticationToken)

        if (usernamePasswordAuthenticationToken.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            log.debug(String.format("Auto login %s successfully!", username));
        }

        return this.getJwtToken(username)
    }

    fun getJwtToken(username: String) : String? {
        val secretKey = "mySecretKey"
        val grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER")

        val token = Jwts
            .builder()
            .setId("softtekJWT")
            .setSubject(username)
            .claim("authorities",
                grantedAuthorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()))
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + 600000))
            .signWith(
                SignatureAlgorithm.HS512,
                secretKey.byteInputStream().readBytes()).compact();

        return "Bearer $token"

    }

    @Throws(JwtException::class)
    fun verifyToken(token: String) : String {
        val secretKey = "mySecretKey"

        val claims = Jwts.parser()
            .setSigningKey(secretKey.byteInputStream().readBytes())
            .parseClaimsJws(token).getBody()

        val subject = claims.getSubject()

        println("ID: " + claims.getId())
        println("Subject: " + claims.getSubject())
        println("Issuer: " + claims.getIssuer())
        println("Expiration: " + claims.getExpiration())

        return subject
    }

}