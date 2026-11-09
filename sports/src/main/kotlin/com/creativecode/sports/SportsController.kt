package com.creativecode.sports

import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("\${sports.api-path:/sports/v1}")
class SportsController(private val store: PostgresSportsStore, private val service: BookingService, private val auth: SportsAuth) {
    private fun actor(r: HttpServletRequest) = r.getAttribute("sports.actor") as Actor
    @PostMapping("/auth/login") fun login(@RequestBody input: LoginInput, r: HttpServletRequest) = auth.login(input,r.remoteAddr)
    @PostMapping("/auth/logout") fun logout(r: HttpServletRequest) { auth.logout(r.getHeader("Authorization")) }
    @GetMapping("/me") fun me(r: HttpServletRequest) = actor(r)
    @GetMapping("/settings") fun settings(r: HttpServletRequest) = store.settings(actor(r).tenantId)
    @PutMapping("/settings") fun settings(r: HttpServletRequest, @RequestBody value: Settings) = store.transaction {
        val a=actor(r); a.admin(); validSettings(value); store.saveSettings(a.tenantId,value); store.audit(a,a.tenantId,"SETTINGS_UPDATED",null); value
    }
    @GetMapping("/venues") fun venues(r: HttpServletRequest) = store.venues(actor(r).tenantId)
    @PostMapping("/venues") fun venue(r: HttpServletRequest, @RequestBody v: Venue) = store.transaction {
        val a=actor(r); a.admin(); checkSports(v.name.isNotBlank() && v.name.length<=120 && (v.address?.length?:0)<=250,"INVALID_VENUE","Sede inválida")
        store.saveVenue(a.tenantId,v); store.audit(a,v.id,"VENUE_SAVED",null); v
    }
    @GetMapping("/courts") fun courts(r: HttpServletRequest) = store.courts(actor(r).tenantId)
    @PostMapping("/courts") fun court(r: HttpServletRequest, @RequestBody c: Court) = store.transaction {
        val a=actor(r); a.admin()
        checkSports(c.name.isNotBlank() && c.name.length<=120 && c.sport.isNotBlank() && c.sport.length<=80 && c.capacity in 1..100 && c.durationMinutes in 5..720 && c.color.matches(Regex("#[0-9a-fA-F]{6}")),"INVALID_COURT","Configuración de cancha inválida")
        if (store.courts(a.tenantId).any { it.id==c.id }) store.lockCourts(a.tenantId,listOf(c.id))
        store.saveCourt(a.tenantId,c); store.audit(a,c.id,"COURT_SAVED",null); c
    }
    @GetMapping("/reservations") fun reservations(r: HttpServletRequest,@RequestParam from: String,@RequestParam to: String,@RequestParam(required=false) courtId: UUID?) = service.list(actor(r),Instant.parse(from),Instant.parse(to)).filter { courtId==null || it.courtId==courtId }
    @GetMapping("/pending-confirmations") fun pending(r: HttpServletRequest,@RequestParam from: String,@RequestParam to: String) = service.list(actor(r),Instant.parse(from),Instant.parse(to)).filter { it.status==Status.PENDING }
    @PostMapping("/reservations") fun create(r: HttpServletRequest,@RequestBody input: BookingInput) = service.create(actor(r),input)
    @PatchMapping("/reservations/{id}") fun edit(r: HttpServletRequest,@PathVariable id: UUID,@RequestBody input: BookingInput) = service.edit(actor(r),id,input)
    @PostMapping("/reservations/{id}/{operation}") fun action(r: HttpServletRequest,@PathVariable id: UUID,@PathVariable operation: String,@RequestBody input: Action) = service.action(actor(r),id,operation,input)
    @GetMapping("/operations/today") fun operations(r: HttpServletRequest): Map<String,Any> {
        val a=actor(r); val now=Instant.now(); val settings=store.settings(a.tenantId)
        val zone=java.time.ZoneId.of(settings.timezone); val date=now.atZone(zone).toLocalDate()
        val start=date.atStartOfDay(zone).toInstant(); val end=date.plusDays(2).atStartOfDay(zone).toInstant()
        return mapOf("serverTime" to now,"settings" to settings,"courts" to store.courts(a.tenantId),"active" to store.active(a.tenantId),"reservations" to store.bookings(a.tenantId,start,end))
    }
    @GetMapping("/clients") fun clients(r: HttpServletRequest) = store.contacts(actor(r).tenantId,false)
    @GetMapping("/teachers") fun teachers(r: HttpServletRequest) = store.contacts(actor(r).tenantId,true)
    @PostMapping("/clients") fun client(r: HttpServletRequest,@RequestBody c: Contact) = contact(actor(r),c,false)
    @PostMapping("/teachers") fun teacher(r: HttpServletRequest,@RequestBody c: Contact) = contact(actor(r),c,true)
    private fun contact(a: Actor,c: Contact,teacher: Boolean): Contact = store.transaction {
        if (teacher) a.admin()
        checkSports(c.name.isNotBlank() && c.name.length<=150 && (c.phone?.length?:0)<=30 && (c.notes?.length?:0)<=2000,"INVALID_CONTACT","Datos de contacto inválidos")
        store.saveContact(a.tenantId,c,teacher); store.audit(a,c.id,if(teacher) "TEACHER_SAVED" else "CLIENT_SAVED",null); c
    }
    @GetMapping("/clients/{id}/history") fun history(r: HttpServletRequest,@PathVariable id: UUID) = store.history(actor(r).tenantId,id)
    @GetMapping("/loyalty") fun loyalty(r: HttpServletRequest) = LoyaltyService(store).all(actor(r))
    @PutMapping("/clients/{id}/loyalty") fun loyalty(r: HttpServletRequest,@PathVariable id: UUID,@RequestBody input: LoyaltyChange) = LoyaltyService(store).configure(actor(r),id,input)
    @PostMapping("/clients/{id}/loyalty/progress") fun loyaltyProgress(r: HttpServletRequest,@PathVariable id: UUID,@RequestBody input: LoyaltyProgress) = LoyaltyService(store).progress(actor(r),id,input)
    @PostMapping("/clients/{id}/loyalty/exception") fun loyaltyException(r: HttpServletRequest,@PathVariable id: UUID,@RequestBody input: LoyaltyException) = LoyaltyService(store).exception(actor(r),id,input)
    @GetMapping("/clients/{id}/loyalty/history") fun loyaltyHistory(r: HttpServletRequest,@PathVariable id: UUID) = store.loyaltyHistory(actor(r).tenantId,id)
    @GetMapping("/pricing-rules") fun prices(r: HttpServletRequest) = store.pricing(actor(r).tenantId)
    @PostMapping("/pricing-rules") fun price(r: HttpServletRequest,@RequestBody p: Pricing) = store.transaction {
        val a=actor(r); a.admin(); store.lockCourts(a.tenantId,listOf(p.courtId))
        checkSports(p.weekday in 1..7 && p.fromMinute in 0..1439 && p.toMinute in 1..1440 && p.toMinute>p.fromMinute && p.amount.signum()>=0 && p.amount.precision()<=14 && p.amount.scale()<=2,"INVALID_PRICE","Tarifa inválida")
        checkSports(store.pricing(a.tenantId).none { it.id!=p.id && it.courtId==p.courtId && it.weekday==p.weekday && it.fromMinute<p.toMinute && it.toMinute>p.fromMinute },"PRICE_OVERLAP","Ya existe una tarifa en esa franja",409)
        store.savePricing(a.tenantId,p); store.audit(a,p.id,"PRICE_SAVED",null); p
    }
    @GetMapping("/recurrence-rules") fun recurrences(r: HttpServletRequest) = store.recurrences(actor(r).tenantId)
    @PostMapping("/recurrence-rules") fun recurrence(r: HttpServletRequest,@RequestBody rule: Recurrence) = service.generate(actor(r),rule)
    @GetMapping("/users") fun users(r: HttpServletRequest) = auth.users(actor(r))
    @PostMapping("/users") fun user(r: HttpServletRequest,@RequestBody input: UserInput) = mapOf("id" to auth.create(actor(r),input))
    @PostMapping("/users/{id}/deactivate") fun deactivate(r: HttpServletRequest,@PathVariable id: UUID) = auth.deactivate(actor(r),id)
}
