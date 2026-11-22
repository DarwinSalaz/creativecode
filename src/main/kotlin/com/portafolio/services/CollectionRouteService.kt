package com.portafolio.services

import com.portafolio.dtos.*
import com.portafolio.entities.ApplicationUser
import com.portafolio.repositories.RelUserWalletRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CollectionRouteService(
    private val jdbcTemplate: JdbcTemplate,
    private val relUserWalletRepository: RelUserWalletRepository
) {
    private val routeStatuses = setOf("draft", "active", "completed")
    private val stopStatuses = setOf("pending", "visited", "paid", "not_found", "rescheduled")

    fun suggestions(user: ApplicationUser, walletId: Int, routeDate: LocalDate): List<CollectionRouteSuggestionResponse> {
        validateAccess(user, walletId)
        return jdbcTemplate.query(suggestionSql(), arrayOf(walletId, routeDate)) { rs, _ -> mapSuggestion(rs, routeDate) }
    }

    fun findAll(user: ApplicationUser): List<CollectionRouteResponse> = jdbcTemplate.query(
        """SELECT r.route_id, r.wallet_id, w.name wallet_name, r.route_date, r.name, r.status, r.created_at
           FROM collection_routes r JOIN wallets w ON w.wallet_id = r.wallet_id
           WHERE r.application_user_id = ? ORDER BY r.route_date DESC, r.route_id DESC""",
        arrayOf(user.applicationUserId)
    ) { rs, _ -> mapRoute(rs, emptyList()) }

    fun findOne(user: ApplicationUser, routeId: Long): CollectionRouteResponse {
        val route = findOwnedRoute(user, routeId)
        val stops = jdbcTemplate.query(
            """SELECT st.route_stop_id, st.service_id, st.customer_id,
                      trim(concat(c.name, ' ', coalesce(c.last_name, ''))) customer_name,
                      c.cellphone, c.address, s.next_payment_date, s.fee_value, s.debt,
                      st.position, st.status, st.observation
               FROM collection_route_stops st
               JOIN services s ON s.service_id = st.service_id
               JOIN customers c ON c.customer_id = st.customer_id
               WHERE st.route_id = ? ORDER BY st.position, st.route_stop_id""",
            arrayOf(routeId)
        ) { rs, _ -> mapStop(rs) }
        return route.copy(stops = stops)
    }

    @Transactional
    fun create(user: ApplicationUser, request: CollectionRouteCreateRequest): CollectionRouteResponse {
        validateAccess(user, request.walletId)
        if (request.serviceIds.isEmpty()) throw IllegalArgumentException("Selecciona al menos una cuenta")
        val available = suggestions(user, request.walletId, request.routeDate).associateBy { it.serviceId }
        val selected = request.serviceIds.distinct().map { available[it] ?: throw IllegalArgumentException("La cuenta $it no está disponible para esta ruta") }
        val routeName = request.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Ruta ${request.routeDate}"
        val routeId = jdbcTemplate.queryForObject(
            "INSERT INTO collection_routes(application_user_id, wallet_id, route_date, name, status) VALUES (?, ?, ?, ?, 'draft') RETURNING route_id",
            arrayOf(user.applicationUserId, request.walletId, request.routeDate, routeName), Long::class.java
        ) ?: throw IllegalStateException("No fue posible crear la ruta")
        selected.forEachIndexed { index, item ->
            jdbcTemplate.update(
                "INSERT INTO collection_route_stops(route_id, service_id, customer_id, position, status) VALUES (?, ?, ?, ?, 'pending')",
                routeId, item.serviceId, item.customerId, index + 1
            )
        }
        return findOne(user, routeId)
    }

    fun updateRouteStatus(user: ApplicationUser, routeId: Long, status: String): CollectionRouteResponse {
        if (!routeStatuses.contains(status)) throw IllegalArgumentException("Estado de ruta inválido")
        findOwnedRoute(user, routeId)
        jdbcTemplate.update("UPDATE collection_routes SET status = ?, updated_at = now() WHERE route_id = ?", status, routeId)
        return findOne(user, routeId)
    }

    @Transactional
    fun reorder(user: ApplicationUser, routeId: Long, stopIds: List<Long>): CollectionRouteResponse {
        findOwnedRoute(user, routeId)
        val currentIds = jdbcTemplate.queryForList(
            "SELECT route_stop_id FROM collection_route_stops WHERE route_id = ? ORDER BY position", Long::class.java, routeId
        )
        if (currentIds.toSet() != stopIds.toSet() || currentIds.size != stopIds.size) {
            throw IllegalArgumentException("La lista de paradas no coincide con la ruta")
        }
        jdbcTemplate.update("UPDATE collection_route_stops SET position = position + 100000 WHERE route_id = ?", routeId)
        stopIds.forEachIndexed { index, stopId ->
            jdbcTemplate.update("UPDATE collection_route_stops SET position = ? WHERE route_id = ? AND route_stop_id = ?", index + 1, routeId, stopId)
        }
        return findOne(user, routeId)
    }

    fun updateStop(user: ApplicationUser, routeId: Long, stopId: Long, request: CollectionRouteStopStatusRequest): CollectionRouteResponse {
        if (!stopStatuses.contains(request.status)) throw IllegalArgumentException("Estado de visita inválido")
        findOwnedRoute(user, routeId)
        val changed = jdbcTemplate.update(
            """UPDATE collection_route_stops SET status = ?, observation = ?,
               visited_at = CASE WHEN ? = 'pending' THEN NULL ELSE now() END
               WHERE route_id = ? AND route_stop_id = ?""",
            request.status, request.observation?.trim(), request.status, routeId, stopId
        )
        if (changed == 0) throw IllegalArgumentException("La parada no pertenece a la ruta")
        return findOne(user, routeId)
    }

    private fun validateAccess(user: ApplicationUser, walletId: Int) {
        if (user.userProfileId !in listOf(1, 3)) throw IllegalArgumentException("El perfil no puede gestionar rutas de cobro")
        val walletIds = relUserWalletRepository.findAllWalletsByUser(user.applicationUserId)?.map { it.walletId } ?: emptyList()
        if (!walletIds.contains(walletId)) throw IllegalArgumentException("La cartera no está asociada al usuario")
    }

    private fun findOwnedRoute(user: ApplicationUser, routeId: Long): CollectionRouteResponse = jdbcTemplate.query(
        """SELECT r.route_id, r.wallet_id, w.name wallet_name, r.route_date, r.name, r.status, r.created_at
           FROM collection_routes r JOIN wallets w ON w.wallet_id = r.wallet_id
           WHERE r.route_id = ? AND r.application_user_id = ?""",
        arrayOf(routeId, user.applicationUserId)
    ) { rs, _ -> mapRoute(rs, emptyList()) }.firstOrNull() ?: throw IllegalArgumentException("Ruta no encontrada")

    private fun suggestionSql() = """SELECT s.service_id, c.customer_id,
        trim(concat(c.name, ' ', coalesce(c.last_name, ''))) customer_name,
        c.cellphone, c.address, s.next_payment_date, s.fee_value, s.debt
        FROM services s JOIN customers c ON c.customer_id = s.customer_id
        WHERE s.wallet_id = ? AND c.active = true AND s.deleted_at IS NULL
          AND s.state IN ('created', 'paying') AND s.debt > 0
          AND s.next_payment_date IS NOT NULL AND s.next_payment_date::date <= ?
        ORDER BY s.next_payment_date, customer_name, s.service_id"""

    private fun mapSuggestion(rs: ResultSet, routeDate: LocalDate) = CollectionRouteSuggestionResponse(
        rs.getLong("service_id"), rs.getLong("customer_id"), rs.getString("customer_name"),
        rs.getString("cellphone"), rs.getString("address"), rs.getTimestamp("next_payment_date")?.toLocalDateTime(),
        rs.getBigDecimal("fee_value"), rs.getBigDecimal("debt"),
        rs.getTimestamp("next_payment_date").toLocalDateTime().toLocalDate().isBefore(routeDate)
    )

    private fun mapRoute(rs: ResultSet, stops: List<CollectionRouteStopResponse>) = CollectionRouteResponse(
        rs.getLong("route_id"), rs.getInt("wallet_id"), rs.getString("wallet_name"),
        rs.getDate("route_date").toLocalDate(), rs.getString("name"), rs.getString("status"),
        rs.getTimestamp("created_at").toLocalDateTime(), stops
    )

    private fun mapStop(rs: ResultSet) = CollectionRouteStopResponse(
        rs.getLong("route_stop_id"), rs.getLong("service_id"), rs.getLong("customer_id"),
        rs.getString("customer_name"), rs.getString("cellphone"), rs.getString("address"),
        rs.getTimestamp("next_payment_date")?.toLocalDateTime(), rs.getBigDecimal("fee_value"),
        rs.getBigDecimal("debt"), rs.getInt("position"), rs.getString("status"), rs.getString("observation")
    )
}
