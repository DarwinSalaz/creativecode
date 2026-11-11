package com.creativecode.sports

import org.junit.Test
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class BookingServiceTest {
    private val tenant=UUID.randomUUID()
    private val actor=Actor(UUID.randomUUID(),tenant,"Operador",Role.EMPLOYEE)
    private val admin=actor.copy(role=Role.ADMIN)
    private val court=Court(name="Cancha 1")
    private val other=Court(name="Cancha 2")
    private val start=Instant.parse("2026-11-03T00:00:00Z") // Monday 19:00 Bogotá
    private val store=MemoryStore().apply { courts[tenant]=mutableListOf(court,other) }
    private fun service(now: Instant=start.plusSeconds(420))=BookingService(store,Clock.fixed(now,ZoneOffset.UTC))
    private fun input(c: Court=court)=BookingInput(c.id,"Equipo",start,start.plusSeconds(3600),status=Status.CONFIRMED)
    private fun error(code: String, block: () -> Unit) { try { block(); fail("Expected $code") } catch(e: SportsError) { assertEquals(code,e.code) } }
    @Test fun `new booking defaults to pending and requires confirmation before start`() {
        val b=service().create(actor,BookingInput(court.id,"Laura",start,start.plusSeconds(3600)))
        assertEquals(Status.PENDING,b.status)
        error("INVALID_STATE"){service().action(actor,b.id,"start",Action(b.version))}
        val confirmed=service().action(actor,b.id,"confirm",Action(b.version))
        assertEquals(Status.IN_PROGRESS,service().action(actor,b.id,"start",Action(confirmed.version)).status)
    }
    @Test fun `person name is required even when a team is supplied`() {
        error("INVALID_INPUT") { service().create(actor,input().copy(guestName="  ",teamName="Los amigos")) }
    }
    @Test fun `team is optional and never replaces the customer name`() {
        val b=service().create(actor,input().copy(guestName="Laura Gómez",teamName=" Los amigos "))
        assertEquals("Laura Gómez",b.guestName); assertEquals("Los amigos",b.teamName)
        val edited=service().edit(actor,b.id,input().copy(guestName="Laura Gómez",version=0,teamName="  "))
        assertEquals("Laura Gómez",edited.guestName); assertNull(edited.teamName)
        error("INVALID_TEAM_NAME") { service().create(actor,input(other).copy(teamName="x".repeat(151))) }
    }
    @Test fun `registered client's name is authoritative`() {
        val c=Contact(name="Carlos Pérez")
        store.people[tenant]=listOf(c)
        val b=service().create(actor,input().copy(guestName="Un equipo",clientId=c.id,teamName="Los amigos"))
        assertEquals("Carlos Pérez",b.guestName)
        assertEquals("Los amigos",b.teamName)
    }
    @Test fun `overlap blocks but adjacent and other courts are independent`() {
        service().create(actor,input())
        error("OVERLAP") { service().create(actor,input().copy(start=start.plusSeconds(1800),end=start.plusSeconds(5400))) }
        service().create(actor,input(other))
        service().create(actor,input().copy(start=start.plusSeconds(3600),end=start.plusSeconds(7200)))
        assertEquals(3,store.rows.size)
    }
    @Test fun `pending fixed reservation blocks`() {
        service().create(actor,input().copy(kind=Kind.FIXED,status=Status.PENDING))
        error("OVERLAP") { service().create(actor,input()) }
    }
    @Test fun `cancellation frees interval and retains record`() {
        val b=service().create(actor,input())
        service().action(actor,b.id,"cancel-rain",Action(b.version))
        service().create(actor,input())
        assertEquals(2,store.rows.size)
        assertEquals(Status.CANCELLED_RAIN,store.rows[b.id]!!.second.status)
    }
    @Test fun `tenant cannot read edit or reserve another tenant court`() {
        val b=service().create(actor,input()); val foreign=actor.copy(tenantId=UUID.randomUUID())
        assertTrue(service().list(foreign,start,start.plusSeconds(3600)).isEmpty())
        error("COURT_NOT_FOUND") { service().create(foreign,input()) }
        error("NOT_FOUND") { service().edit(foreign,b.id,input().copy(version=0)) }
        error("NOT_FOUND") { service().action(foreign,b.id,"start",Action(0)) }
    }
    @Test fun `foreign client and teacher references are rejected`() {
        error("CLIENT_NOT_FOUND") { service().create(actor,input().copy(clientId=UUID.randomUUID())) }
        error("TEACHER_NOT_FOUND") { service().create(actor,input().copy(teacherId=UUID.randomUUID(),kind=Kind.CLASS)) }
    }
    @Test fun `real start remains independent from schedule and retry is idempotent`() {
        val b=service().create(actor,input()); val running=service().action(actor,b.id,"start",Action(0,deviceId="desk"))
        assertEquals(start,running.start); assertEquals(start.plusSeconds(420),running.actualStartedAt)
        assertEquals(running,service().action(actor,b.id,"start",Action(0,deviceId="other")))
        assertEquals(start.plusSeconds(2220),running.actualStartedAt!!.plusSeconds(1800))
        assertEquals(start.plusSeconds(4020),running.actualStartedAt!!.plusSeconds(3600))
    }
    @Test fun `late next match gets no new tolerance`() {
        val first=service().create(actor,input())
        val next=service().create(actor,input().copy(start=start.plusSeconds(3600),end=start.plusSeconds(7200)))
        val running=service(start.plusSeconds(900)).action(actor,first.id,"start",Action(0))
        service(start.plusSeconds(4500)).action(actor,first.id,"finish",Action(running.version))
        error("START_CONFIRMATION_REQUIRED") { service(start.plusSeconds(4560)).action(actor,next.id,"start",Action(0)) }
        val nextRunning=service(start.plusSeconds(4560)).action(actor,next.id,"start",Action(0,exceptionConfirmed=true))
        assertEquals(start.plusSeconds(4560),nextRunning.actualStartedAt)
        assertEquals(start.plusSeconds(3600),nextRunning.start)
    }
    @Test fun `only one running match per court`() {
        val b=service().create(actor,input()); service().action(actor,b.id,"start",Action(0))
        val next=service().create(actor,input().copy(start=start.plusSeconds(3600),end=start.plusSeconds(7200)))
        error("COURT_BUSY") { service(start.plusSeconds(3600)).action(actor,next.id,"start",Action(0)) }
    }
    @Test fun `finish water and cancellation retries do not duplicate events`() {
        val b=service().create(actor,input()); val running=service().action(actor,b.id,"start",Action(0))
        val wet=service().action(actor,b.id,"water-delivered",Action(running.version))
        assertEquals(wet,service().action(actor,b.id,"water-delivered",Action(running.version)))
        val done=service(start.plusSeconds(4020)).action(actor,b.id,"finish",Action(wet.version))
        assertEquals(done,service().action(actor,b.id,"finish",Action(wet.version)))
        assertEquals(4,store.events.size)
    }
    @Test fun `cancel competing with start cannot overwrite state`() {
        val b=service().create(actor,input()); service().action(actor,b.id,"start",Action(0))
        error("STALE_VERSION") { service().action(actor,b.id,"cancel",Action(0)) }
        error("INVALID_STATE") { service().action(actor,b.id,"cancel",Action(1)) }
    }
    @Test fun `stale editing and target court conflict are rejected`() {
        val b=service().create(actor,input()); service().create(actor,input(other))
        error("STALE_VERSION") { service().edit(actor,b.id,input().copy(version=4)) }
        error("OVERLAP") { service().edit(actor,b.id,input(other).copy(version=0)) }
        assertEquals(court.id,store.rows[b.id]!!.second.courtId)
    }
    @Test fun `no show waits original tolerance`() {
        val b=service().create(actor,input())
        error("TOO_EARLY") { service().action(actor,b.id,"mark-no-show",Action(0)) }
        assertEquals(Status.NO_SHOW,service(start.plusSeconds(900)).action(actor,b.id,"mark-no-show",Action(0)).status)
        service().create(actor,input())
    }
    @Test fun `administrative blocks require admin and cannot start`() {
        error("FORBIDDEN") { service().create(actor,input().copy(kind=Kind.BLOCK)) }
        val b=service().create(admin,input().copy(kind=Kind.BLOCK))
        error("INVALID_STATE") { service().action(admin,b.id,"start",Action(0)) }
    }
    @Test fun `employee cannot edit or cancel an administrative block`() {
        val b=service().create(admin,input().copy(kind=Kind.BLOCK))
        error("FORBIDDEN") { service().edit(actor,b.id,input().copy(version=0,kind=Kind.NORMAL)) }
        error("FORBIDDEN") { service().action(actor,b.id,"cancel",Action(0)) }
        assertEquals(Kind.BLOCK,store.rows[b.id]!!.second.kind)
    }
    @Test fun `series generation is idempotent and instances remain independent`() {
        val rule=Recurrence(courtId=court.id,guestName="Fijo",weekday=1,localTime="19:00",fromDate="2026-11-02",untilDate="2026-11-16")
        val generated=service().generate(admin,rule)
        assertEquals(3,generated.size); assertTrue(generated.all { it.status==Status.PENDING })
        assertTrue(service().generate(admin,rule).isEmpty())
        service().action(actor,generated.first().id,"cancel",Action(0))
        assertTrue(service().generate(admin,rule).isEmpty())
        assertEquals(Status.PENDING,store.rows[generated.last().id]!!.second.status)
    }
    @Test fun `series conflict rolls back all generated instances`() {
        service().create(actor,input().copy(start=start.plusSeconds(7*86400),end=start.plusSeconds(7*86400+3600)))
        val rule=Recurrence(courtId=court.id,guestName="Fijo",weekday=1,localTime="19:00",fromDate="2026-11-02",untilDate="2026-11-16")
        error("OVERLAP") { service().generate(admin,rule) }
        assertEquals(1,store.rows.size); assertTrue(store.rules.isEmpty())
    }
    @Test fun `outside hours requires explicit confirmation`() {
        val early=input().copy(start=Instant.parse("2026-11-02T14:00:00Z"),end=Instant.parse("2026-11-02T15:00:00Z"))
        error("OUTSIDE_HOURS") { service().create(actor,early) }
        service().create(actor,early.copy(outsideHoursConfirmed=true))
    }
    @Test fun `price snapshot survives later pricing changes`() {
        store.prices.add(Pricing(courtId=court.id,weekday=1,fromMinute=1080,toMinute=1440,amount=java.math.BigDecimal("90000")))
        val b=service().create(actor,input()); store.prices.clear()
        assertEquals(java.math.BigDecimal("90000"),service().edit(actor,b.id,input().copy(version=0,guestName="Otro nombre")).price)
    }
}

/** Test double only: no JDBC, no records or structures written to any database. */
internal class MemoryStore: SportsStore {
    val courts=mutableMapOf<UUID,MutableList<Court>>()
    val rows=mutableMapOf<UUID,Pair<UUID,Booking>>()
    val events=mutableListOf<String>()
    val rules=mutableListOf<Recurrence>()
    val prices=mutableListOf<Pricing>()
    val people=mutableMapOf<UUID,List<Contact>>()
    @Synchronized override fun <T> transaction(block: () -> T): T {
        val old=rows.toMap(); val e=events.toList(); val r=rules.toList();val l=loyalty.toMap();val le=ledger.toList()
        try { return block() } catch(ex: Exception) { rows.clear(); rows.putAll(old); events.clear(); events.addAll(e); rules.clear(); rules.addAll(r);loyalty.clear();loyalty.putAll(l);ledger.clear();ledger.addAll(le); throw ex }
    }
    val loyalty=mutableMapOf<Pair<UUID,UUID>,LoyaltyState>()
    val ledger=mutableListOf<Pair<UUID,LoyaltyEvent>>()
    override fun lockClient(tenant: UUID,id: UUID){checkSports(contacts(tenant,false).any { it.id==id },"CLIENT_NOT_FOUND","Cliente no encontrado",404)}
    override fun loyaltyState(tenant: UUID,id: UUID)=loyalty[Pair(tenant,id)]
    override fun loyaltyStates(tenant: UUID)=loyalty.filterKeys { it.first==tenant }.map { Pair(it.key.second,it.value) }
    override fun saveLoyalty(tenant: UUID,id: UUID,state: LoyaltyState){loyalty[Pair(tenant,id)]=state}
    override fun appendLoyalty(actor: Actor,event: LoyaltyEvent){ledger.add(Pair(actor.tenantId,event))}
    override fun loyaltyHistory(tenant: UUID,id: UUID)=ledger.filter { it.first==tenant && it.second.clientId==id }.map { it.second }
    override fun settings(tenant: UUID)=Settings("Deportes")
    override fun saveSettings(tenant: UUID,value: Settings) {}
    override fun courts(tenant: UUID)=courts[tenant]?.toList()?:emptyList()
    override fun lockCourts(tenant: UUID,ids: List<UUID>) { ids.forEach { id -> checkSports(courts(tenant).any { it.id==id },"COURT_NOT_FOUND","No encontrada",404) } }
    override fun saveCourt(tenant: UUID,court: Court) {}
    override fun bookings(tenant: UUID,from: Instant,to: Instant)=rows.values.filter { it.first==tenant && it.second.start<to && it.second.end>from }.map { it.second }
    override fun booking(tenant: UUID,id: UUID)=rows[id]?.takeIf { it.first==tenant }?.second
    override fun lockBooking(tenant: UUID,id: UUID)=booking(tenant,id)
    override fun saveBooking(actor: Actor,booking: Booking,isNew: Boolean) { rows[booking.id]=Pair(actor.tenantId,booking) }
    override fun activeOnCourt(tenant: UUID,court: UUID)=rows.values.find { it.first==tenant && it.second.courtId==court && it.second.status==Status.IN_PROGRESS }?.second
    override fun contacts(tenant: UUID,teachers: Boolean)=if(teachers) emptyList<Contact>() else people[tenant]?:emptyList()
    override fun saveContact(tenant: UUID,contact: Contact,teachers: Boolean) {}
    override fun pricing(tenant: UUID)=prices.toList()
    override fun savePricing(tenant: UUID,value: Pricing) {}
    override fun audit(actor: Actor,id: UUID,action: String,reason: String?) { events.add(action) }
    override fun recurrences(tenant: UUID)=rules.filter { r -> courts(tenant).any { it.id==r.courtId } }
    override fun saveRecurrence(tenant: UUID,value: Recurrence) { rules.removeAll { it.id==value.id }; rules.add(value) }
    override fun generated(tenant: UUID,recurrence: UUID,start: Instant)=rows.values.any { it.first==tenant && it.second.recurrenceId==recurrence && it.second.start==start }
}
