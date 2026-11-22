package com.portafolio.controllers

import com.portafolio.dtos.*
import com.portafolio.entities.ApplicationUser
import com.portafolio.repositories.ApplicationUserRepository
import com.portafolio.services.ApplicationUserService
import com.portafolio.services.CollectionRouteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"])
class CollectionRouteController(
    private val collectionRouteService: CollectionRouteService,
    private val applicationUserService: ApplicationUserService,
    private val applicationUserRepository: ApplicationUserRepository
) {
    @GetMapping("/collection-routes")
    fun list(@RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.findAll(user)
    }

    @GetMapping("/collection-routes/{routeId}")
    fun detail(@PathVariable routeId: Long, @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.findOne(user, routeId)
    }

    @GetMapping("/collection-routes/suggestions")
    fun suggestions(@RequestParam("wallet_id") walletId: Int, @RequestParam("route_date") routeDate: LocalDate,
                    @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.suggestions(user, walletId, routeDate)
    }

    @PostMapping("/collection-routes")
    fun create(@RequestBody request: CollectionRouteCreateRequest, @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.create(user, request)
    }

    @PutMapping("/collection-routes/{routeId}/status")
    fun status(@PathVariable routeId: Long, @RequestBody request: CollectionRouteStatusRequest,
               @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.updateRouteStatus(user, routeId, request.status)
    }

    @PutMapping("/collection-routes/{routeId}/reorder")
    fun reorder(@PathVariable routeId: Long, @RequestBody request: CollectionRouteReorderRequest,
                @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.reorder(user, routeId, request.stopIds)
    }

    @PutMapping("/collection-routes/{routeId}/stops/{stopId}")
    fun stop(@PathVariable routeId: Long, @PathVariable stopId: Long, @RequestBody request: CollectionRouteStopStatusRequest,
             @RequestHeader("Authorization") authorization: String) = respond(authorization) { user ->
        collectionRouteService.updateStop(user, routeId, stopId, request)
    }

    private fun respond(authorization: String, action: (ApplicationUser) -> Any): ResponseEntity<Any> {
        return try {
            val token = if (authorization.contains("Bearer")) authorization.split(" ")[1] else authorization
            val username = applicationUserService.verifyToken(token)
            val user = applicationUserRepository.findByUsername(username)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            ResponseEntity.ok(action(user))
        } catch (error: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (error.message ?: "Solicitud inválida")))
        } catch (error: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
