package com.creativecode.sports

import org.junit.Test
import org.junit.Assert.*
import java.time.*
import java.util.UUID
import java.math.BigDecimal

class LoyaltyServiceTest {
    val tenant=UUID.randomUUID()
    val actor=Actor(UUID.randomUUID(),tenant,"Admin",Role.ADMIN)
    val client=Contact(name="Laura")
    val court=Court(name="Cancha")
    private val store=MemoryStore().apply { people[tenant]=listOf(client);courts[tenant]=mutableListOf(court) }
    val now=Instant.parse("2026-11-03T00:00:00Z")
    fun loyalty(at: Instant=now)=LoyaltyService(store,Clock.fixed(at,ZoneOffset.UTC))
    fun booking(at: Instant=now)=Booking(courtId=court.id,guestName=client.name,clientId=client.id,start=at,end=at.plusSeconds(3600),status=Status.IN_PROGRESS,actualStartedAt=at,price=BigDecimal("100000"))
    fun finish(b: Booking=booking(),at: Instant=now): Booking {
        store.rows[b.id]=Pair(tenant,b)
        return BookingService(store,Clock.fixed(at,ZoneOffset.UTC)).action(actor,b.id,"finish",Action(b.version))
    }
    fun configure(percent: Int=100,required: Int=5)=loyalty().configure(actor,client.id,LoyaltyChange(LoyaltyPolicy(requiredMatches=required,rewardPercent=percent)))
    fun error(code: String,block: ()->Unit){try{block();fail(code)}catch(e: SportsError){assertEquals(code,e.code)}}
    @Test fun `five finished games earn next free and retries never double count`() {
        configure()
        repeat(5){val b=finish();BookingService(store,Clock.fixed(now,ZoneOffset.UTC)).action(actor,b.id,"finish",Action(0))}
        assertTrue(store.loyaltyState(tenant,client.id)!!.rewardReady)
        assertEquals(5,store.ledger.count { it.second.type=="PLAYED" })
        val reward=finish();assertEquals(100,reward.rewardPercent);assertEquals(0,reward.finalPrice!!.signum())
        assertEquals(BigDecimal("100000"),reward.price)
        assertEquals(0,store.loyaltyState(tenant,client.id)!!.played)
        assertEquals(0,finish().rewardPercent)
    }
    @Test fun `ten matches earn half price`() {configure(50,10);repeat(10){finish()};assertEquals(BigDecimal("50000"),finish().finalPrice)}
    @Test fun `cancelled bookings preserve earned reward`() {
        configure();repeat(5){finish()}
        val b=booking().copy(status=Status.CONFIRMED,actualStartedAt=null);store.rows[b.id]=Pair(tenant,b)
        BookingService(store,Clock.fixed(now,ZoneOffset.UTC)).action(actor,b.id,"cancel",Action(0))
        assertTrue(store.loyaltyState(tenant,client.id)!!.rewardReady)
        assertEquals(100,finish(at=now.plusSeconds(100*86400L)).rewardPercent)
    }
    @Test fun `gap resets progress but preserves ledger`() {
        configure();repeat(3){finish()}
        assertTrue(loyalty(now.plusSeconds(22*86400L)).all(actor).single().expired)
        finish(at=now.plusSeconds(22*86400L))
        assertEquals(1,store.loyaltyState(tenant,client.id)!!.played)
        assertEquals(1,store.ledger.count { it.second.type=="CYCLE_EXPIRED" })
        assertEquals(4,store.ledger.count { it.second.type=="PLAYED" })
    }
    @Test fun `rain exception extends gap and is audited`() {
        configure();repeat(3){finish()}
        val s=store.loyaltyState(tenant,client.id)!!
        loyalty().exception(actor,client.id,LoyaltyException(now.plusSeconds(30*86400L),"Lluvia",s.version))
        finish(at=now.plusSeconds(25*86400L));assertEquals(4,store.loyaltyState(tenant,client.id)!!.played)
        assertEquals("Lluvia",store.ledger.find { it.second.type=="GAP_EXCEPTION" }!!.second.reason)
    }
    @Test fun `employee other tenant invalid policy and stale config are rejected`() {
        error("FORBIDDEN"){loyalty().configure(actor.copy(role=Role.EMPLOYEE),client.id,LoyaltyChange(LoyaltyPolicy()))}
        error("CLIENT_NOT_FOUND"){loyalty().configure(actor.copy(tenantId=UUID.randomUUID()),client.id,LoyaltyChange(LoyaltyPolicy()))}
        error("INVALID_LOYALTY"){loyalty().configure(actor,client.id,LoyaltyChange(LoyaltyPolicy(requiredMatches=1)))}
        configure();error("STALE_VERSION"){configure()}
        finish();val s=store.loyaltyState(tenant,client.id)!!
        error("LOYALTY_CYCLE_ACTIVE"){loyalty().configure(actor,client.id,LoyaltyChange(s.policy.copy(rewardPercent=50),s.version))}
    }
    @Test fun `disabled policies classes and anonymous games do not count`() {
        configure();val s=store.loyaltyState(tenant,client.id)!!
        loyalty().configure(actor,client.id,LoyaltyChange(s.policy.copy(active=false),s.version));finish()
        assertEquals(0,store.loyaltyState(tenant,client.id)!!.played)
        val disabled=store.loyaltyState(tenant,client.id)!!
        loyalty().configure(actor,client.id,LoyaltyChange(disabled.policy.copy(active=true),disabled.version))
        finish(booking().copy(clientId=null));finish(booking().copy(kind=Kind.CLASS));assertEquals(0,store.loyaltyState(tenant,client.id)!!.played)
    }
}
