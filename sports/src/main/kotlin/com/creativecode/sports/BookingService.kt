package com.creativecode.sports

import java.time.*
import java.util.UUID

class BookingService(private val store: SportsStore, private val clock: Clock = Clock.systemUTC()) {
    fun list(a: Actor, from: Instant, to: Instant): List<Booking> {
        checkSports(to > from && Duration.between(from, to).toDays() <= 93, "INVALID_RANGE", "Consulta máximo 93 días")
        return store.transaction {
            RecurrenceService(store,clock).ensure(a,from,to)
            store.bookings(a.tenantId, from, to)
        }
    }
    fun create(a: Actor, input: BookingInput, recurrence: UUID? = null): Booking = store.transaction {
        store.lockCourts(a.tenantId, listOf(input.courtId))
        validate(a, input)
        if(recurrence==null) RecurrenceService(store,clock).ensure(a,input.start,input.end,listOf(input.courtId))
        val b = Booking(courtId = input.courtId, guestName = customerName(a, input), phone = input.phone,
            clientId = input.clientId, teacherId = input.teacherId, kind = input.kind, status = input.status,
            start = input.start, end = input.end, notes = input.notes, price = price(a, input), recurrenceId = recurrence, teamName = input.teamName?.trim()?.takeIf { it.isNotEmpty() })
        conflicts(a, b)
        store.saveBooking(a, b, true)
        store.audit(a, b.id, "CREATED", null)
        b
    }
    fun edit(a: Actor, id: UUID, input: BookingInput): Booking = store.transaction {
        val initial = find(a, id)
        store.lockCourts(a.tenantId, (listOf(initial.courtId, input.courtId)+store.recurrences(a.tenantId).filter { it.id==initial.recurrenceId }.map { it.courtId }).distinct().sortedBy { it.toString() })
        val old = store.lockBooking(a.tenantId, id) ?: throw SportsError("NOT_FOUND", "Reserva no encontrada", 404)
        checkSports(old.courtId == initial.courtId, "CONFLICT", "La reserva cambió; actualiza la vista", 409)
        checkSports(input.version == old.version, "STALE_VERSION", "La reserva cambió; actualiza la vista", 409)
        checkSports(old.status in listOf(Status.PENDING, Status.CONFIRMED), "INVALID_STATE", "Solo puedes editar reservas pendientes o confirmadas", 409)
        if (old.kind == Kind.BLOCK) a.admin()
        validate(a, input)
        RecurrenceService(store,clock).ensure(a,input.start,input.end,listOf(input.courtId))
        val b = old.copy(courtId = input.courtId, guestName = customerName(a, input), phone = input.phone, clientId = input.clientId,
            teacherId = input.teacherId, kind = input.kind, status = input.status, start = input.start, end = input.end,
            notes = input.notes, teamName = input.teamName?.trim()?.takeIf { it.isNotEmpty() }, price = if (old.start == input.start && old.end == input.end && old.courtId == input.courtId) old.price else price(a, input), version = old.version + 1)
        conflicts(a, b)
        if(old.start!=b.start) RecurrenceService(store,clock).excludeMoved(a,old)
        store.saveBooking(a, b, false)
        store.audit(a, id, "UPDATED", null)
        b
    }
    fun action(a: Actor, id: UUID, operation: String, input: Action): Booking = store.transaction {
        val initial = find(a, id)
        store.lockCourts(a.tenantId, listOf(initial.courtId))
        val b = store.lockBooking(a.tenantId, id) ?: throw SportsError("NOT_FOUND", "Reserva no encontrada", 404)
        checkSports(initial.courtId == b.courtId, "CONFLICT", "La cancha cambió; actualiza la vista", 409)
        if (b.kind == Kind.BLOCK) a.admin()
        val now = clock.instant()
        checkSports((input.reason?.length ?: 0) <= 500 && (input.deviceId?.length ?: 0) <= 100, "INVALID_INPUT", "Texto demasiado largo")
        // A retry of an already completed action is successful, even with the original version.
        val done = when(operation) {
            "start" -> b.actualStartedAt != null
            "finish" -> b.status == Status.FINISHED
            "water-delivered" -> b.waterDeliveredAt != null
            "confirm" -> b.status == Status.CONFIRMED
            "cancel" -> b.status == Status.CANCELLED_CLIENT
            "cancel-rain" -> b.status == Status.CANCELLED_RAIN
            "mark-no-show" -> b.status == Status.NO_SHOW
            else -> false
        }
        if (done) return@transaction b
        checkSports(b.version == input.version, "STALE_VERSION", "La reserva cambió; actualiza la vista", 409)
        var next = when(operation) {
            "confirm" -> { requireState(b, Status.PENDING); b.copy(status = Status.CONFIRMED) }
            "cancel", "cancel-rain", "mark-no-show" -> {
                checkSports(b.status in listOf(Status.PENDING, Status.CONFIRMED), "INVALID_STATE", "El partido no se puede cancelar en este estado", 409)
                if (operation == "mark-no-show") checkSports(now >= b.start.plusSeconds(store.settings(a.tenantId).toleranceMinutes * 60L), "TOO_EARLY", "Aún no termina la tolerancia", 409)
                b.copy(status = when(operation) { "cancel" -> Status.CANCELLED_CLIENT; "cancel-rain" -> Status.CANCELLED_RAIN; else -> Status.NO_SHOW })
            }
            "start" -> {
                requireState(b, Status.CONFIRMED)
                checkSports(b.kind != Kind.BLOCK, "INVALID_STATE", "Un bloqueo no es un partido")
                checkSports(store.activeOnCourt(a.tenantId, b.courtId) == null, "COURT_BUSY", "Finaliza el partido actual antes de iniciar otro", 409)
                val tolerance = store.settings(a.tenantId).toleranceMinutes * 60L
                checkSports((now >= b.start && now <= b.start.plusSeconds(tolerance)) || input.exceptionConfirmed, "START_CONFIRMATION_REQUIRED", "Confirma el inicio fuera de la hora o tolerancia programada", 409)
                b.copy(status = Status.IN_PROGRESS, actualStartedAt = now, soundDevice = input.deviceId)
            }
            "finish" -> { requireState(b, Status.IN_PROGRESS); b.copy(status = Status.FINISHED, actualEndedAt = now) }
            "water-delivered" -> { requireState(b, Status.IN_PROGRESS); b.copy(waterDeliveredAt = now) }
            else -> throw SportsError("UNKNOWN_ACTION", "Acción desconocida", 404)
        }.copy(version = b.version + 1)
        if(operation=="finish") next=LoyaltyService(store,clock).finish(a,next)
        store.saveBooking(a, next, false)
        store.audit(a, id, operation, input.reason)
        next
    }
    private fun requireState(b: Booking, status: Status) = checkSports(b.status == status, "INVALID_STATE", "Acción incompatible con el estado actual", 409)
    private fun find(a: Actor, id: UUID) = store.booking(a.tenantId, id) ?: throw SportsError("NOT_FOUND", "Reserva no encontrada", 404)
    private fun validate(a: Actor, b: BookingInput) {
        checkSports((b.teamName?.length ?: 0) <= 150, "INVALID_TEAM_NAME", "El nombre del equipo admite hasta 150 caracteres")
        val c = store.courts(a.tenantId).find { it.id == b.courtId && it.active }
        checkSports(c != null, "COURT_NOT_FOUND", "Cancha no disponible", 404)
        checkSports(b.end > b.start && Duration.between(b.start, b.end).seconds in 300..43200, "INVALID_INTERVAL", "Duración permitida: 5 minutos a 12 horas")
        checkSports(b.guestName.isNotBlank() && b.guestName.length <= 150 && (b.phone?.length ?: 0) <= 30 && (b.notes?.length ?: 0) <= 2000, "INVALID_INPUT", "Revisa nombre, teléfono y notas")
        checkSports(b.status in listOf(Status.PENDING, Status.CONFIRMED), "INVALID_STATE", "Estado inicial inválido")
        if (b.kind == Kind.BLOCK) a.admin()
        checkSports(b.kind != Kind.CLASS || b.teacherId != null, "TEACHER_REQUIRED", "Selecciona un profesor")
        checkSports(b.clientId == null || store.contacts(a.tenantId, false).any { it.id == b.clientId }, "CLIENT_NOT_FOUND", "Cliente no encontrado", 404)
        checkSports(b.teacherId == null || store.contacts(a.tenantId, true).any { it.id == b.teacherId }, "TEACHER_NOT_FOUND", "Profesor no encontrado", 404)
        val local = b.start.atZone(ZoneId.of(store.settings(a.tenantId).timezone))
        val end = b.end.atZone(local.zone)
        val outside = local.dayOfWeek.value <= 5 && (local.hour < 16 || end.toLocalDate() != local.toLocalDate() || end.toLocalTime() > LocalTime.of(23, 0))
        checkSports(!outside || b.outsideHoursConfirmed, "OUTSIDE_HOURS", "Confirma la reserva fuera del horario de referencia")
    }
    private fun customerName(a: Actor, b: BookingInput): String = if (b.clientId == null) b.guestName.trim()
        else store.contacts(a.tenantId, false).find { it.id == b.clientId }?.name
            ?: throw SportsError("CLIENT_NOT_FOUND", "Cliente no encontrado", 404)
    private fun conflicts(a: Actor, b: Booking) {
        checkSports(store.bookings(a.tenantId, b.start, b.end).none { it.id != b.id && it.courtId == b.courtId && it.blocks() && it.start < b.end && it.end > b.start }, "OVERLAP", "La cancha ya está ocupada en ese horario", 409)
    }
    private fun price(a: Actor, b: BookingInput): java.math.BigDecimal? {
        val local = b.start.atZone(ZoneId.of(store.settings(a.tenantId).timezone))
        val minute = local.hour * 60 + local.minute
        return store.pricing(a.tenantId).find { it.courtId == b.courtId && it.weekday == local.dayOfWeek.value && minute >= it.fromMinute && minute < it.toMinute }?.amount
    }
    fun generate(a: Actor, rule: Recurrence): List<Booking> = RecurrenceService(store,clock).generate(a,rule)
}
