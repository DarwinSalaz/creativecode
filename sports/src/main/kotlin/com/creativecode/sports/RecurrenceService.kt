package com.creativecode.sports

import java.time.*
import java.util.UUID

/** Weekly rules are durable; only requested calendar windows are materialized. Court locks
 * serialize every writer, including conflicts with occurrences not materialized yet. */
class RecurrenceService(private val store: SportsStore, private val clock: Clock = Clock.systemUTC()) {
    private val farFuture = LocalDate.of(9999,12,31)
    private fun zone(a: Actor) = ZoneId.of(store.settings(a.tenantId).timezone)
    private fun end(r: Recurrence): LocalDate = minOf(r.untilDate?.let { LocalDate.parse(it) }?:farFuture,
        r.stoppedFrom?.let { LocalDate.parse(it).minusDays(1) }?:farFuture)
    private fun dates(r: Recurrence, from: LocalDate, to: LocalDate): List<LocalDate> {
        var d=maxOf(LocalDate.parse(r.fromDate),from)
        val last=minOf(end(r),to)
        d=d.plusDays(((r.weekday-d.dayOfWeek.value+7)%7).toLong())
        val result=mutableListOf<LocalDate>()
        while(d<=last){result.add(d);d=d.plusWeeks(1)}
        return result
    }
    private fun start(r: Recurrence,d: LocalDate,z: ZoneId)=d.atTime(LocalTime.parse(r.localTime)).atZone(z).toInstant()
    private fun occurs(r: Recurrence, from: Instant,to: Instant,z: ZoneId): List<Instant> =
        dates(r,from.atZone(z).toLocalDate().minusDays(1),to.atZone(z).toLocalDate()).map { start(r,it,z) }
            .filter { it<to && it.plusSeconds(r.durationMinutes*60L)>from && it !in r.excludedStarts }

    fun ensure(a: Actor,from: Instant,to: Instant,courts: List<UUID>?=null): List<Booking> {
        val ids=(courts?:store.courts(a.tenantId).filter { it.active }.map { it.id }).distinct().sortedBy { it.toString() }
        store.lockCourts(a.tenantId,ids)
        val rules=store.recurrences(a.tenantId).filter { it.courtId in ids && it.automatic }
        val result=mutableListOf<Booking>();val z=zone(a)
        for(r in rules) for(t in occurs(r,from,to,z)) {
            if(!store.generated(a.tenantId,r.id,t)) result.add(BookingService(store,clock).create(a,
                BookingInput(r.courtId,r.guestName,t,t.plusSeconds(r.durationMinutes*60L),phone=r.phone,
                    clientId=r.clientId,teacherId=r.teacherId,kind=r.kind,status=r.status,notes=r.notes,
                    outsideHoursConfirmed=r.outsideHoursConfirmed,teamName=r.teamName),r.id))
        }
        return result
    }
    fun generate(a: Actor,input: Recurrence): List<Booking> = store.transaction {
        val r=input.copy(untilDate=input.untilDate?.takeIf { it.isNotBlank() },automatic=true)
        val from: LocalDate
        try { from=LocalDate.parse(r.fromDate);LocalTime.parse(r.localTime);end(r) }
        catch(e: DateTimeException){throw SportsError("INVALID_RECURRENCE","Revisa las fechas y la hora de repetición")}
        checkSports(r.weekday in 1..7 && r.durationMinutes in 5..720 && end(r)>=from && from.year in 2000..9998 && (r.untilDate==null || end(r).year<=9998),
            "INVALID_RECURRENCE","Revisa el día, la duración y la fecha final")
        checkSports(r.kind in listOf(Kind.FIXED,Kind.CLASS) && r.stoppedFrom==null && r.excludedStarts.isEmpty() && r.version==0,
            "INVALID_RECURRENCE","Configuración de serie inválida")
        store.lockCourts(a.tenantId,listOf(r.courtId))
        val existing=store.recurrences(a.tenantId).find { it.id==r.id }
        checkSports(existing==null || existing==r,"RECURRENCE_IMMUTABLE","La serie ya cambió; crea otra programación",409)
        val z=zone(a)
        if(existing==null) {
            // Weekly overlap repeats every seven days. Include the preceding day for midnight crossings.
            for(other in store.recurrences(a.tenantId).filter { it.courtId==r.courtId }) {
                val first=maxOf(from,LocalDate.parse(other.fromDate))
                val last=minOf(end(r),end(other),first.plusDays(14))
                if(last<first)continue
                for(t in occurs(r.copy(excludedStarts=emptyList()),first.atStartOfDay(z).toInstant(),last.plusDays(1).atStartOfDay(z).toInstant(),z))
                    checkSports(occurs(other.copy(excludedStarts=emptyList()),t,t.plusSeconds(r.durationMinutes*60L),z).isEmpty(),"OVERLAP","Otra serie ocupa ese horario",409)
            }
            // Check all already booked dates, including dates beyond the initial generation window.
            for(b in store.bookings(a.tenantId,from.atStartOfDay(z).toInstant(),end(r).plusDays(1).atStartOfDay(z).toInstant())
                .filter { it.courtId==r.courtId && it.blocks() })
                checkSports(occurs(r,b.start,b.end,z).isEmpty(),"OVERLAP","La cancha ya está ocupada el ${b.start.atZone(z).toLocalDate()} a las ${b.start.atZone(z).toLocalTime()}",409)
            store.saveRecurrence(a.tenantId,r)
            store.audit(a,r.id,"RECURRENCE_CREATED",null)
        }
        val until=minOf(end(r),from.plusDays(92))
        checkSports(dates(r,from,end(r).coerceAtMost(from.plusDays(7))).isNotEmpty(),"INVALID_RECURRENCE","El rango no incluye el día seleccionado")
        ensure(a,from.atStartOfDay(z).toInstant(),until.plusDays(1).atStartOfDay(z).toInstant(),listOf(r.courtId))
            .filter { it.recurrenceId==r.id }
    }
    fun excludeMoved(a: Actor,b: Booking) {
        val id=b.recurrenceId?:return
        val r=store.recurrences(a.tenantId).find { it.id==id }?:return
        store.saveRecurrence(a.tenantId,r.copy(excludedStarts=(r.excludedStarts+b.start).distinct(),version=r.version+1))
    }
    fun stop(a: Actor,id: UUID,input: StopRecurrence): Recurrence = store.transaction {
        a.admin()
        store.recurrences(a.tenantId).find { it.id==id }?:throw SportsError("NOT_FOUND","Serie no encontrada",404)
        store.lockCourts(a.tenantId,store.courts(a.tenantId).map { it.id }.sortedBy { it.toString() })
        val r=store.recurrences(a.tenantId).first { it.id==id }
        checkSports(input.version==r.version,"STALE_VERSION","La serie cambió; actualiza la vista",409)
        val date=try { LocalDate.parse(input.fromDate) } catch(e: DateTimeException){throw SportsError("INVALID_RECURRENCE","Fecha inválida")}
        checkSports(date>=clock.instant().atZone(zone(a)).toLocalDate() && date.year<=9998,"INVALID_RECURRENCE","Solo puedes terminar la serie desde hoy o una fecha futura")
        checkSports(r.stoppedFrom==null || date<=LocalDate.parse(r.stoppedFrom),"INVALID_RECURRENCE","Una serie terminada no se puede extender")
        val next=r.copy(stoppedFrom=date.toString(),version=r.version+1)
        store.saveRecurrence(a.tenantId,next)
        for(b in store.bookings(a.tenantId,date.atStartOfDay(zone(a)).toInstant(),farFuture.atStartOfDay(zone(a)).toInstant())
            .filter { it.recurrenceId==id && it.start>=date.atStartOfDay(zone(a)).toInstant() && it.status in listOf(Status.PENDING,Status.CONFIRMED) }) {
            store.saveBooking(a,b.copy(status=Status.CANCELLED_CLIENT,version=b.version+1),false)
            store.audit(a,b.id,"cancel","Fin de serie desde $date")
        }
        store.audit(a,id,"RECURRENCE_STOPPED","Desde $date")
        next
    }
}
