package com.creativecode.sports

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/** All SQL explicitly qualifies sports. No search_path changes, ORM scanning or DDL. */
class PostgresSportsStore(dataSource: DataSource, private val json: ObjectMapper): SportsStore {
    val jdbc = JdbcTemplate(dataSource).apply { queryTimeout = 5 }
    private val tx = TransactionTemplate(DataSourceTransactionManager(dataSource)).apply { timeout = 10 }
    override fun <T> transaction(block: () -> T): T {
        var result: Any? = null
        tx.execute { result = block(); null }
        @Suppress("UNCHECKED_CAST") return result as T
    }
    override fun settings(tenant: UUID): Settings = jdbc.query("SELECT settings FROM sports.tenants WHERE id=?", RowMapper { r, _ -> json.readValue(r.getString(1), Settings::class.java) }, tenant).firstOrNull() ?: throw SportsError("TENANT_NOT_FOUND", "Negocio no disponible", 403)
    override fun saveSettings(tenant: UUID, value: Settings) { jdbc.update("UPDATE sports.tenants SET settings=?::jsonb WHERE id=?", json.writeValueAsString(value), tenant) }
    override fun courts(tenant: UUID): List<Court> = jdbc.query("SELECT * FROM sports.courts WHERE tenant_id=? ORDER BY name", RowMapper { r, _ -> Court(uuid(r, "id")!!, r.getString("name"), r.getString("sport"), r.getInt("capacity"), r.getInt("duration_minutes"), r.getString("color"), r.getBoolean("active"), uuid(r,"venue_id")) }, tenant)
    fun venues(tenant: UUID): List<Venue> = jdbc.query("SELECT id,name,address FROM sports.venues WHERE tenant_id=? ORDER BY name", RowMapper { r, _ -> Venue(uuid(r,"id")!!,r.getString("name"),r.getString("address")) },tenant)
    fun saveVenue(tenant: UUID, v: Venue) { jdbc.update("INSERT INTO sports.venues(tenant_id,id,name,address) VALUES (?,?,?,?) ON CONFLICT (tenant_id,id) DO UPDATE SET name=excluded.name,address=excluded.address",tenant,v.id,v.name,v.address) }
    override fun lockCourts(tenant: UUID, ids: List<UUID>) {
        ids.distinct().sortedBy { it.toString() }.forEach { id ->
            val rows = jdbc.queryForList("SELECT id FROM sports.courts WHERE tenant_id=? AND id=? FOR UPDATE", tenant, id)
            checkSports(rows.size == 1, "COURT_NOT_FOUND", "Cancha no encontrada", 404)
        }
    }
    override fun saveCourt(tenant: UUID, court: Court) {
        jdbc.update("""INSERT INTO sports.courts (tenant_id,id,name,sport,capacity,duration_minutes,color,active,venue_id) VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (tenant_id,id) DO UPDATE SET name=excluded.name,sport=excluded.sport,capacity=excluded.capacity,duration_minutes=excluded.duration_minutes,color=excluded.color,active=excluded.active,venue_id=excluded.venue_id""", tenant, court.id, court.name, court.sport, court.capacity, court.durationMinutes, court.color, court.active, court.venueId)
    }
    private val bookingMapper = RowMapper<Booking> { r, _ -> Booking(uuid(r,"id")!!, uuid(r,"court_id")!!, r.getString("guest_name"), r.getString("phone"), uuid(r,"client_id"), uuid(r,"teacher_id"), Kind.valueOf(r.getString("kind")), Status.valueOf(r.getString("status")), instant(r,"scheduled_start_at")!!, instant(r,"scheduled_end_at")!!, r.getString("notes"), r.getBigDecimal("price_snapshot"), r.getInt("version"), instant(r,"actual_started_at"), instant(r,"actual_ended_at"), instant(r,"water_delivered_at"), r.getString("sound_device"), uuid(r,"recurrence_id"), r.getString("team_name"),r.getInt("reward_percent")) }
    override fun bookings(tenant: UUID, from: Instant, to: Instant): List<Booking> = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND scheduled_start_at < ? AND scheduled_end_at > ? ORDER BY scheduled_start_at", bookingMapper, tenant, Timestamp.from(to), Timestamp.from(from))
    override fun booking(tenant: UUID, id: UUID): Booking? = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND id=?", bookingMapper, tenant, id).firstOrNull()
    override fun lockBooking(tenant: UUID, id: UUID): Booking? = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND id=? FOR UPDATE", bookingMapper, tenant, id).firstOrNull()
    override fun activeOnCourt(tenant: UUID, court: UUID): Booking? = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND court_id=? AND status='IN_PROGRESS'", bookingMapper, tenant, court).firstOrNull()
    fun active(tenant: UUID): List<Booking> = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND status='IN_PROGRESS'", bookingMapper, tenant)
    fun history(tenant: UUID, client: UUID): List<Booking> = jdbc.query("SELECT * FROM sports.reservations WHERE tenant_id=? AND client_id=? ORDER BY scheduled_start_at DESC LIMIT 200", bookingMapper, tenant, client)
    override fun saveBooking(actor: Actor, booking: Booking, isNew: Boolean) {
        val b = booking
        if (isNew) jdbc.update("""INSERT INTO sports.reservations (tenant_id,id,court_id,guest_name,phone,client_id,teacher_id,kind,status,scheduled_start_at,scheduled_end_at,notes,price_snapshot,version,recurrence_id,created_by,updated_by,team_name)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", actor.tenantId,b.id,b.courtId,b.guestName,b.phone,b.clientId,b.teacherId,b.kind.name,b.status.name,Timestamp.from(b.start),Timestamp.from(b.end),b.notes,b.price,b.version,b.recurrenceId,actor.id,actor.id,b.teamName)
        else {
            val count = jdbc.update("""UPDATE sports.reservations SET court_id=?,guest_name=?,team_name=?,phone=?,client_id=?,teacher_id=?,kind=?,status=?,scheduled_start_at=?,scheduled_end_at=?,notes=?,price_snapshot=?,version=?,actual_started_at=?,actual_ended_at=?,water_delivered_at=?,sound_device=?,reward_percent=?,updated_by=?,updated_at=now()
                WHERE tenant_id=? AND id=? AND version=?""", b.courtId,b.guestName,b.teamName,b.phone,b.clientId,b.teacherId,b.kind.name,b.status.name,Timestamp.from(b.start),Timestamp.from(b.end),b.notes,b.price,b.version,b.actualStartedAt?.let { Timestamp.from(it) },b.actualEndedAt?.let { Timestamp.from(it) },b.waterDeliveredAt?.let { Timestamp.from(it) },b.soundDevice,b.rewardPercent,actor.id,actor.tenantId,b.id,b.version-1)
            checkSports(count == 1, "STALE_VERSION", "La reserva cambió; actualiza la vista", 409)
        }
    }
    override fun contacts(tenant: UUID, teachers: Boolean): List<Contact> {
        val table = if (teachers) "teachers" else "clients"
        return jdbc.query("SELECT id,name,phone,notes FROM sports.$table WHERE tenant_id=? ORDER BY name", RowMapper { r, _ -> Contact(uuid(r,"id")!!,r.getString("name"),r.getString("phone"),r.getString("notes")) }, tenant)
    }
    override fun saveContact(tenant: UUID, contact: Contact, teachers: Boolean) {
        val table = if (teachers) "teachers" else "clients"
        jdbc.update("INSERT INTO sports.$table (tenant_id,id,name,phone,notes) VALUES (?,?,?,?,?) ON CONFLICT (tenant_id,id) DO UPDATE SET name=excluded.name,phone=excluded.phone,notes=excluded.notes", tenant,contact.id,contact.name,contact.phone,contact.notes)
    }
    override fun pricing(tenant: UUID): List<Pricing> = jdbc.query("SELECT * FROM sports.pricing_rules WHERE tenant_id=? ORDER BY weekday,from_minute", RowMapper { r, _ -> Pricing(uuid(r,"id")!!,uuid(r,"court_id")!!,r.getInt("weekday"),r.getInt("from_minute"),r.getInt("to_minute"),r.getBigDecimal("amount")) }, tenant)
    override fun savePricing(tenant: UUID, value: Pricing) { val p=value; jdbc.update("INSERT INTO sports.pricing_rules (tenant_id,id,court_id,weekday,from_minute,to_minute,amount) VALUES (?,?,?,?,?,?,?) ON CONFLICT (tenant_id,id) DO UPDATE SET court_id=excluded.court_id,weekday=excluded.weekday,from_minute=excluded.from_minute,to_minute=excluded.to_minute,amount=excluded.amount",tenant,p.id,p.courtId,p.weekday,p.fromMinute,p.toMinute,p.amount) }
    override fun audit(actor: Actor, id: UUID, action: String, reason: String?) { jdbc.update("INSERT INTO sports.audit_events (tenant_id,id,actor_id,entity_id,action,reason) VALUES (?,?,?,?,?,?)", actor.tenantId,UUID.randomUUID(),actor.id,id,action,reason) }
    override fun recurrences(tenant: UUID): List<Recurrence> = jdbc.query("SELECT definition FROM sports.recurrence_rules WHERE tenant_id=?", RowMapper { r, _ -> json.readValue(r.getString(1),Recurrence::class.java) },tenant)
    override fun saveRecurrence(tenant: UUID, value: Recurrence) { jdbc.update("INSERT INTO sports.recurrence_rules (tenant_id,id,court_id,definition) VALUES (?,?,?,?::jsonb) ON CONFLICT (tenant_id,id) DO UPDATE SET definition=excluded.definition",tenant,value.id,value.courtId,json.writeValueAsString(value)) }
    override fun generated(tenant: UUID, recurrence: UUID, start: Instant): Boolean = jdbc.queryForObject("SELECT count(*) FROM sports.reservations WHERE tenant_id=? AND recurrence_id=? AND scheduled_start_at=?", Long::class.java,tenant,recurrence,Timestamp.from(start))!! > 0
    override fun lockClient(tenant: UUID,id: UUID) {
        checkSports(jdbc.queryForList("SELECT id FROM sports.clients WHERE tenant_id=? AND id=? FOR UPDATE",tenant,id).size==1,"CLIENT_NOT_FOUND","Cliente no encontrado",404)
    }
    override fun loyaltyState(tenant: UUID,id: UUID): LoyaltyState? = jdbc.query("SELECT state FROM sports.loyalty_policies WHERE tenant_id=? AND client_id=?",RowMapper { r, _ -> json.readValue(r.getString(1),LoyaltyState::class.java) },tenant,id).firstOrNull()
    override fun loyaltyStates(tenant: UUID): List<Pair<UUID,LoyaltyState>> = jdbc.query("SELECT client_id,state FROM sports.loyalty_policies WHERE tenant_id=?",RowMapper { r, _ -> Pair(uuid(r,"client_id")!!,json.readValue(r.getString("state"),LoyaltyState::class.java)) },tenant)
    override fun saveLoyalty(tenant: UUID,id: UUID,state: LoyaltyState) { jdbc.update("INSERT INTO sports.loyalty_policies(tenant_id,client_id,state) VALUES (?,?,?::jsonb) ON CONFLICT (tenant_id,client_id) DO UPDATE SET state=excluded.state",tenant,id,json.writeValueAsString(state)) }
    override fun appendLoyalty(actor: Actor,event: LoyaltyEvent) { jdbc.update("INSERT INTO sports.loyalty_ledger(tenant_id,id,client_id,reservation_id,event_type,actor_id,definition) VALUES (?,?,?,?,?,?,?::jsonb)",actor.tenantId,event.id,event.clientId,event.reservationId,event.type,actor.id,json.writeValueAsString(event)) }
    override fun loyaltyHistory(tenant: UUID,id: UUID): List<LoyaltyEvent> = jdbc.query("SELECT definition FROM sports.loyalty_ledger WHERE tenant_id=? AND client_id=? ORDER BY created_at DESC,id DESC LIMIT 200",RowMapper { r, _ -> json.readValue(r.getString(1),LoyaltyEvent::class.java) },tenant,id)
    private fun uuid(r: ResultSet, name: String): UUID? = r.getString(name)?.let { UUID.fromString(it) }
    private fun instant(r: ResultSet, name: String): Instant? = r.getTimestamp(name)?.toInstant()
}
