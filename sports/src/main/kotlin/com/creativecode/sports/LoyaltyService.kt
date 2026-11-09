package com.creativecode.sports

import java.time.Clock
import java.time.Instant
import java.util.UUID

data class LoyaltyPolicy(val requiredMatches: Int = 5, val rewardPercent: Int = 100, val maxGapDays: Int = 21, val active: Boolean = true)
data class LoyaltyState(val policy: LoyaltyPolicy = LoyaltyPolicy(), val played: Int = 0, val rewardReady: Boolean = false, val lastPlayedAt: Instant? = null, val graceUntil: Instant? = null, val version: Int = 0)
data class LoyaltyChange(val policy: LoyaltyPolicy, val version: Int = 0)
data class LoyaltyProgress(val played: Int, val reason: String, val version: Int)
data class LoyaltyException(val until: Instant, val reason: String, val version: Int)
data class LoyaltyEvent(val id: UUID = UUID.randomUUID(), val clientId: UUID, val reservationId: UUID? = null, val type: String, val at: Instant, val reason: String? = null, val rewardPercent: Int = 0, val state: LoyaltyState)
data class LoyaltySummary(val clientId: UUID, val state: LoyaltyState, val effectivePlayed: Int, val expired: Boolean)

/** All mutations run inside the caller's transaction and serialize on the client row. */
class LoyaltyService(private val store: SportsStore, private val clock: Clock = Clock.systemUTC()) {
    private fun expired(s: LoyaltyState, now: Instant): Boolean = !s.rewardReady && s.played > 0 && s.lastPlayedAt != null &&
        now > s.lastPlayedAt.plusSeconds(s.policy.maxGapDays * 86400L) && (s.graceUntil == null || now > s.graceUntil)
    fun summary(id: UUID, s: LoyaltyState): LoyaltySummary = LoyaltySummary(id,s,if(expired(s,clock.instant())) 0 else s.played,expired(s,clock.instant()))
    fun all(a: Actor) = store.loyaltyStates(a.tenantId).map { summary(it.first,it.second) }
    fun configure(a: Actor, id: UUID, change: LoyaltyChange): LoyaltySummary = store.transaction {
        a.admin(); store.lockClient(a.tenantId,id)
        val old=store.loyaltyState(a.tenantId,id)
        val p=change.policy
        checkSports(p.requiredMatches in listOf(5,10) && p.rewardPercent in listOf(50,100) && p.maxGapDays in 1..365,"INVALID_LOYALTY","Selecciona 5 o 10 partidos, beneficio del 50% o 100% y plazo de 1 a 365 días")
        checkSports(change.version==(old?.version?:0),"STALE_VERSION","La promoción cambió; actualiza la vista",409)
        checkSports(old==null || (old.played==0 && !old.rewardReady) || (old.policy.requiredMatches==p.requiredMatches && old.policy.rewardPercent==p.rewardPercent),"LOYALTY_CYCLE_ACTIVE","Termina el ciclo antes de cambiar la meta o el beneficio",409)
        val next=(old?:LoyaltyState()).copy(policy=p,version=(old?.version?:0)+1)
        record(a,id,next,"POLICY_UPDATED")
        summary(id,next)
    }
    fun progress(a: Actor,id: UUID,input: LoyaltyProgress): LoyaltySummary = store.transaction {
        a.admin();store.lockClient(a.tenantId,id)
        val old=store.loyaltyState(a.tenantId,id)?:throw SportsError("NOT_FOUND","Cliente sin promoción",404)
        checkSports(input.version==old.version,"STALE_VERSION","La promoción cambió; actualiza antes de ajustar el avance",409)
        checkSports(input.played in 0..old.policy.requiredMatches && input.reason.isNotBlank() && input.reason.length<=500,"INVALID_PROGRESS","Indica un avance entre cero y la meta y un motivo de máximo 500 caracteres")
        val next=old.copy(played=input.played,rewardReady=input.played==old.policy.requiredMatches,lastPlayedAt=if(input.played>0)clock.instant() else null,graceUntil=null,version=old.version+1)
        record(a,id,next,"PROGRESS_ADJUSTED",reason="Avance ${old.played} → ${input.played}. "+input.reason.trim())
        summary(id,next)
    }
    fun exception(a: Actor,id: UUID,input: LoyaltyException): LoyaltySummary = store.transaction {
        a.admin();store.lockClient(a.tenantId,id)
        val old=store.loyaltyState(a.tenantId,id)?:throw SportsError("NOT_FOUND","Cliente sin promoción",404)
        checkSports(input.version==old.version,"STALE_VERSION","La promoción cambió; actualiza la vista",409)
        checkSports(input.reason.isNotBlank() && input.reason.length<=500 && input.until>clock.instant() && input.until<=clock.instant().plusSeconds(365*86400L),"INVALID_EXCEPTION","Indica un motivo y un plazo futuro de máximo un año")
        val next=old.copy(graceUntil=input.until,version=old.version+1)
        record(a,id,next,"GAP_EXCEPTION",reason=input.reason.trim())
        summary(id,next)
    }
    fun finish(a: Actor,b: Booking): Booking {
        val id=b.clientId?:return b
        if(b.kind!=Kind.NORMAL && b.kind!=Kind.FIXED)return b
        store.lockClient(a.tenantId,id)
        var s=store.loyaltyState(a.tenantId,id)?:return b
        if(!s.policy.active)return b
        val now=clock.instant()
        if(expired(s,now)){
            s=s.copy(played=0,rewardReady=false,graceUntil=null,version=s.version+1)
            record(a,id,s,"CYCLE_EXPIRED",b.id)
        }
        val reward=if(s.rewardReady)s.policy.rewardPercent else 0
        val count=if(s.rewardReady)0 else s.played+1
        val next=s.copy(played=count,rewardReady=count>=s.policy.requiredMatches,lastPlayedAt=now,graceUntil=null,version=s.version+1)
        record(a,id,next,if(reward>0)"REDEEMED" else "PLAYED",b.id,reward=reward)
        return b.copy(rewardPercent=reward)
    }
    private fun record(a: Actor,id: UUID,s: LoyaltyState,type: String,booking: UUID?=null,reason: String?=null,reward: Int=0){
        store.saveLoyalty(a.tenantId,id,s)
        store.appendLoyalty(a,LoyaltyEvent(clientId=id,reservationId=booking,type=type,at=clock.instant(),reason=reason,rewardPercent=reward,state=s))
    }
}
