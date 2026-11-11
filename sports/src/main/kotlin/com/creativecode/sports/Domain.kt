package com.creativecode.sports

import java.time.Instant
import java.time.ZoneId
import java.math.BigDecimal
import java.util.UUID

enum class Role { ADMIN, EMPLOYEE }
enum class Status { PENDING, CONFIRMED, IN_PROGRESS, FINISHED, CANCELLED_CLIENT, CANCELLED_RAIN, NO_SHOW }
enum class Kind { NORMAL, FIXED, CLASS, BLOCK }
data class Actor(val id: UUID, val tenantId: UUID, val name: String, val role: Role)
data class Court(val id: UUID = UUID.randomUUID(), val name: String, val sport: String = "Fútbol", val capacity: Int = 10, val durationMinutes: Int = 60, val color: String = "#167d69", val active: Boolean = true, val venueId: UUID? = null)
data class Venue(val id: UUID = UUID.randomUUID(), val name: String, val address: String? = null)
data class Contact(val id: UUID = UUID.randomUUID(), val name: String, val phone: String? = null, val notes: String? = null)
data class Settings(val name: String, val timezone: String = "America/Bogota", val currency: String = "COP", val toleranceMinutes: Int = 15, val waterMinutes: Int = 30, val warningMinutes: Int = 5)
data class Booking(
    val id: UUID = UUID.randomUUID(), val courtId: UUID, val guestName: String,
    val phone: String? = null, val clientId: UUID? = null, val teacherId: UUID? = null,
    val kind: Kind = Kind.NORMAL, val status: Status = Status.PENDING,
    val start: Instant, val end: Instant, val notes: String? = null,
    val price: BigDecimal? = null, val version: Int = 0,
    val actualStartedAt: Instant? = null, val actualEndedAt: Instant? = null,
    val waterDeliveredAt: Instant? = null, val soundDevice: String? = null,
    val recurrenceId: UUID? = null, val teamName: String? = null, val rewardPercent: Int = 0
) { val finalPrice: BigDecimal? get() = price?.multiply(BigDecimal(100-rewardPercent))?.divide(BigDecimal(100)) }
data class BookingInput(val courtId: UUID, val guestName: String, val start: Instant, val end: Instant,
    val phone: String? = null, val clientId: UUID? = null, val teacherId: UUID? = null,
    val kind: Kind = Kind.NORMAL, val status: Status = Status.PENDING, val notes: String? = null,
    val version: Int? = null, val outsideHoursConfirmed: Boolean = false, val teamName: String? = null)
data class Action(val version: Int, val reason: String? = null, val deviceId: String? = null, val exceptionConfirmed: Boolean = false)
data class Pricing(val id: UUID = UUID.randomUUID(), val courtId: UUID, val weekday: Int, val fromMinute: Int, val toMinute: Int, val amount: BigDecimal)
data class Recurrence(val id: UUID = UUID.randomUUID(), val courtId: UUID, val guestName: String, val phone: String? = null,
    val weekday: Int, val localTime: String, val durationMinutes: Int = 60, val fromDate: String, val untilDate: String? = null, val teamName: String? = null, val clientId: UUID? = null,
    val kind: Kind = Kind.FIXED, val teacherId: UUID? = null, val notes: String? = null,
    val status: Status = Status.PENDING, val outsideHoursConfirmed: Boolean = true,
    val automatic: Boolean = false, val stoppedFrom: String? = null, val excludedStarts: List<Instant> = emptyList(), val version: Int = 0)
data class StopRecurrence(val fromDate: String, val version: Int)
class SportsError(val code: String, override val message: String, val statusCode: Int = 400): RuntimeException(message)
fun checkSports(ok: Boolean, code: String, message: String, status: Int = 400) { if (!ok) throw SportsError(code, message, status) }
fun Actor.admin() = checkSports(role == Role.ADMIN, "FORBIDDEN", "Se requiere administrador", 403)
fun Booking.blocks() = status !in listOf(Status.CANCELLED_CLIENT, Status.CANCELLED_RAIN, Status.NO_SHOW)
fun validSettings(s: Settings) {
    checkSports(s.name.isNotBlank() && s.name.length <= 120 && s.currency == "COP", "INVALID_SETTINGS", "Configuración inválida")
    try { ZoneId.of(s.timezone) } catch (e: Exception) { throw SportsError("INVALID_TIMEZONE", "Zona horaria inválida") }
    checkSports(s.toleranceMinutes in 0..60 && s.waterMinutes in 1..180 && s.warningMinutes in 1..30, "INVALID_SETTINGS", "Intervalos inválidos")
}

/** Domain port; no dependency on sales or a particular connection / transaction framework. */
interface SportsStore {
    fun <T> transaction(block: () -> T): T
    fun lockClient(tenant: UUID, id: UUID)
    fun loyaltyState(tenant: UUID, id: UUID): LoyaltyState?
    fun loyaltyStates(tenant: UUID): List<Pair<UUID,LoyaltyState>>
    fun saveLoyalty(tenant: UUID, id: UUID, state: LoyaltyState)
    fun appendLoyalty(actor: Actor, event: LoyaltyEvent)
    fun loyaltyHistory(tenant: UUID, id: UUID): List<LoyaltyEvent>
    fun settings(tenant: UUID): Settings
    fun saveSettings(tenant: UUID, value: Settings)
    fun courts(tenant: UUID): List<Court>
    fun lockCourts(tenant: UUID, ids: List<UUID>)
    fun saveCourt(tenant: UUID, court: Court)
    fun bookings(tenant: UUID, from: Instant, to: Instant): List<Booking>
    fun booking(tenant: UUID, id: UUID): Booking?
    fun lockBooking(tenant: UUID, id: UUID): Booking?
    fun saveBooking(actor: Actor, booking: Booking, isNew: Boolean)
    fun activeOnCourt(tenant: UUID, court: UUID): Booking?
    fun contacts(tenant: UUID, teachers: Boolean): List<Contact>
    fun saveContact(tenant: UUID, contact: Contact, teachers: Boolean)
    fun pricing(tenant: UUID): List<Pricing>
    fun savePricing(tenant: UUID, value: Pricing)
    fun audit(actor: Actor, id: UUID, action: String, reason: String?)
    fun recurrences(tenant: UUID): List<Recurrence>
    fun saveRecurrence(tenant: UUID, value: Recurrence)
    fun generated(tenant: UUID, recurrence: UUID, start: Instant): Boolean
}
