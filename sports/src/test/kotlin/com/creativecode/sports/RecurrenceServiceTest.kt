package com.creativecode.sports

import org.junit.Test
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class RecurrenceServiceTest {
    private val tenant=UUID.randomUUID()
    private val actor=Actor(UUID.randomUUID(),tenant,"Admin",Role.ADMIN)
    private val court=Court(name="Cancha 2")
    private val now=Instant.parse("2026-11-10T12:00:00Z")
    private val clock=Clock.fixed(now,ZoneOffset.UTC)
    private val store=MemoryStore().apply { courts[tenant]=mutableListOf(court) }
    private val bookings=BookingService(store,clock)
    private val series=RecurrenceService(store,clock)
    private fun rule(until:String?=null)=Recurrence(courtId=court.id,guestName="Laura",weekday=2,localTime="21:00",fromDate="2026-11-10",untilDate=until)
    private fun input(b:Booking)=BookingInput(b.courtId,b.guestName,b.start,b.end,kind=b.kind,status=b.status,version=b.version,outsideHoursConfirmed=true)
    private fun error(code:String,fn:()->Unit){try{fn();fail("Expected $code")}catch(e:SportsError){assertEquals(code,e.code)}}
    @Test fun `unbounded rules materialize distant windows idempotently and block direct writes`() {
        val r=rule();val initial=series.generate(actor.copy(role=Role.EMPLOYEE),r)
        assertTrue(initial.size in 13..14)
        val date=Instant.parse("2036-11-11T02:00:00Z") // Monday local: use next day Tuesday
        val t=date.plusSeconds(86400)
        error("OVERLAP"){bookings.create(actor,BookingInput(court.id,"Otra persona",t,t.plusSeconds(3600)))}
        val rows=bookings.list(actor,t,t.plusSeconds(3600))
        assertEquals(1,rows.size);assertEquals("Laura",rows.single().guestName)
        assertEquals(rows,bookings.list(actor,t,t.plusSeconds(3600)))
    }
    @Test fun `cancel one date permits replacement without reviving it or affecting next week`() {
        val r=rule();val first=series.generate(actor,r).first()
        bookings.action(actor,first.id,"cancel",Action(0))
        val replacement=bookings.create(actor,input(first).copy(guestName="Reemplazo",kind=Kind.NORMAL))
        repeat(2){bookings.list(actor,first.start,first.end)}
        assertEquals(2,store.bookings(tenant,first.start,first.end).size)
        assertEquals(replacement.id,store.bookings(tenant,first.start,first.end).single { it.blocks() }.id)
        assertEquals(1,bookings.list(actor,first.start.plusSeconds(7*86400),first.end.plusSeconds(7*86400)).size)
    }
    @Test fun `moving occurrence frees original slot permanently and preserves other dates`() {
        val first=series.generate(actor,rule()).first()
        bookings.edit(actor,first.id,input(first).copy(start=first.start.plusSeconds(3600),end=first.end.plusSeconds(3600)))
        assertTrue(bookings.list(actor,first.start,first.end).isEmpty())
        bookings.create(actor,input(first).copy(guestName="Reemplazo",kind=Kind.NORMAL))
        assertEquals(1,store.bookings(tenant,first.start,first.end).size)
        assertEquals(1,store.rules.single().excludedStarts.size)
    }
    @Test fun `bounded series stops after inclusive end date and can exceed 93 days`() {
        series.generate(actor,rule("2027-11-30"))
        val final=Instant.parse("2027-12-01T02:00:00Z")
        assertEquals(1,bookings.list(actor,final,final.plusSeconds(3600)).size)
        assertTrue(bookings.list(actor,final.plusSeconds(7*86400),final.plusSeconds(7*86400+3600)).isEmpty())
    }
    @Test fun `conflicts beyond initial window rollback rule and adjacent series work`() {
        val r=rule();val t=Instant.parse("2027-12-01T02:00:00Z")
        bookings.create(actor,BookingInput(court.id,"Ocupada",t,t.plusSeconds(3600)))
        error("OVERLAP"){series.generate(actor,r)}
        assertTrue(store.rules.isEmpty());assertEquals(1,store.rows.size)
        series.generate(actor,r.copy(localTime="20:00"))
        error("OVERLAP"){series.generate(actor,rule().copy(fromDate="2030-01-01",localTime="20:30"))}
    }
    @Test fun `ending series cancels future pending dates keeps history and forbids employee`() {
        val r=rule();val first=series.generate(actor,r).first()
        error("FORBIDDEN"){series.stop(actor.copy(role=Role.EMPLOYEE),r.id,StopRecurrence("2026-11-17",0))}
        val stopped=series.stop(actor,r.id,StopRecurrence("2026-11-17",0))
        assertEquals(1,stopped.version)
        assertEquals(Status.PENDING,store.booking(tenant,first.id)!!.status)
        val next=first.start.plusSeconds(7*86400)
        assertTrue(bookings.list(actor,next,next.plusSeconds(3600)).none { it.blocks() })
        bookings.create(actor,BookingInput(court.id,"Otra persona",next,next.plusSeconds(3600)))
        val future=first.start.plusSeconds(700*86400)
        assertTrue(bookings.list(actor,future,future.plusSeconds(3600)).isEmpty())
        error("STALE_VERSION"){series.stop(actor,r.id,StopRecurrence("2026-11-17",0))}
    }
    @Test fun `ending today preserves a running match`() {
        val r=rule();val first=series.generate(actor,r).first()
        val confirmed=bookings.action(actor,first.id,"confirm",Action(0))
        val running=bookings.action(actor,first.id,"start",Action(confirmed.version,exceptionConfirmed=true))
        series.stop(actor,r.id,StopRecurrence("2026-11-10",0))
        assertEquals(running,store.booking(tenant,first.id))
        assertEquals(1,store.rows.values.count { it.second.blocks() })
    }
    @Test fun `invalid dates and tenant cannot create or terminate rules`() {
        error("INVALID_RECURRENCE"){series.generate(actor,rule().copy(fromDate="invalid"))}
        error("INVALID_RECURRENCE"){series.generate(actor,rule("2026-11-09"))}
        error("INVALID_RECURRENCE"){series.generate(actor,rule().copy(weekday=0))}
        error("COURT_NOT_FOUND"){series.generate(actor.copy(tenantId=UUID.randomUUID()),rule())}
        val r=rule();series.generate(actor,r)
        error("NOT_FOUND"){series.stop(actor.copy(tenantId=UUID.randomUUID()),r.id,StopRecurrence("2026-11-17",0))}
    }
    @Test fun `legacy rules remain unchanged on calendar reads`() {
        val r=rule("2026-11-30");store.saveRecurrence(tenant,r)
        assertTrue(bookings.list(actor,now,now.plusSeconds(7*86400)).isEmpty())
        assertEquals(r,store.rules.single())
    }
    @Test fun `overnight series conflict across different weekdays`() {
        series.generate(actor,rule().copy(localTime="23:00",durationMinutes=120))
        error("OVERLAP"){series.generate(actor,rule().copy(weekday=3,localTime="00:30",fromDate="2026-11-11"))}
    }
}
