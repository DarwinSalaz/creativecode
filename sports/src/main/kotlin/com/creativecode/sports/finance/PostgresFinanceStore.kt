package com.creativecode.sports.finance

import com.creativecode.sports.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.RowMapper
import java.math.BigDecimal
import java.sql.Date
import java.sql.ResultSet
import java.util.UUID

/** Uses the sports private pool and transaction manager. All relations stay in sports. */
class PostgresFinanceStore(private val sports:PostgresSportsStore,private val json:ObjectMapper):FinanceStore {
 private val jdbc=sports.jdbc
 private fun id(r:ResultSet,key:String)=r.getObject(key) as UUID
 private fun date(s:String)=Date.valueOf(s)
 override fun <T> transaction(block:()->T):T=sports.transaction(block)
 override fun lock(tenant:UUID){checkSports(jdbc.queryForList("SELECT id FROM sports.tenants WHERE id=? FOR UPDATE",tenant).size==1,"TENANT_NOT_FOUND","Negocio no disponible",403)}
 override fun timezone(tenant:UUID)=sports.settings(tenant).timezone
 override fun people(tenant:UUID)=jdbc.query("SELECT id,name,role,active FROM sports.users WHERE tenant_id=? ORDER BY name",RowMapper { r,_->FinancePerson(id(r,"id"),r.getString("name"),Role.valueOf(r.getString("role")),r.getBoolean("active")) },tenant)
 override fun accounts(tenant:UUID)=jdbc.query("SELECT id,name,kind,owner_id FROM sports.finance_accounts WHERE tenant_id=? ORDER BY kind,name",RowMapper { r,_->FinanceAccount(id(r,"id"),r.getString("name"),r.getString("kind"),r.getObject("owner_id") as UUID?) },tenant)
 override fun saveAccount(tenant:UUID,account:FinanceAccount){jdbc.update("INSERT INTO sports.finance_accounts(tenant_id,id,name,kind,owner_id) VALUES(?,?,?,?,?)",tenant,account.id,account.name,account.kind,account.ownerId)}
 override fun base(tenant:UUID)=jdbc.query("SELECT cash_base FROM sports.finance_settings WHERE tenant_id=?",RowMapper { r,_->r.getBigDecimal(1) },tenant).firstOrNull()?:BigDecimal("150000")
 override fun saveBase(tenant:UUID,amount:BigDecimal){jdbc.update("INSERT INTO sports.finance_settings(tenant_id,cash_base) VALUES(?,?) ON CONFLICT(tenant_id) DO UPDATE SET cash_base=excluded.cash_base",tenant,amount)}
 private val dayMapper=RowMapper { r:ResultSet,_:Int->FinanceDay(id(r,"id"),r.getString("day"),id(r,"actor_id"),r.getBigDecimal("cash"),r.getBigDecimal("cash_base"),json.readValue(r.getString("transfers"),Array<FinanceTransfer>::class.java).toList(),r.getString("note")) }
 override fun days(tenant:UUID,from:String,to:String)=jdbc.query("SELECT * FROM sports.finance_days WHERE tenant_id=? AND day BETWEEN ? AND ? ORDER BY day DESC",dayMapper,tenant,date(from),date(to))
 override fun pendingDays(tenant:UUID,actor:UUID)=jdbc.query("SELECT d.* FROM sports.finance_days d WHERE d.tenant_id=? AND d.actor_id=? AND d.cash>0 AND NOT EXISTS(SELECT 1 FROM sports.finance_handover_days h WHERE h.tenant_id=d.tenant_id AND h.day_id=d.id) ORDER BY d.day",dayMapper,tenant,actor)
 override fun saveDay(tenant:UUID,day:FinanceDay){jdbc.update("INSERT INTO sports.finance_days(tenant_id,id,day,actor_id,cash,cash_base,transfers,note) VALUES(?,?,?,?,?,?,?::jsonb,?)",tenant,day.id,date(day.day),day.actorId,day.cash,day.cashBase,json.writeValueAsString(day.transfers),day.note)}
 private val entryMapper=RowMapper { r:ResultSet,_:Int->FinanceEntry(id(r,"id"),id(r,"document_id"),id(r,"account_id"),r.getString("day"),r.getString("kind"),r.getBigDecimal("amount"),id(r,"actor_id"),r.getString("note"),r.getObject("receipt_id") as UUID?,r.getString("relates_to_month")) }
 override fun entries(tenant:UUID,from:String,to:String)=jdbc.query("SELECT * FROM sports.finance_entries WHERE tenant_id=? AND day BETWEEN ? AND ? ORDER BY day,created_at,id",entryMapper,tenant,date(from),date(to))
 override fun balances(tenant:UUID,until:String):Map<UUID,BigDecimal> = jdbc.query("SELECT account_id,sum(amount) AS balance FROM sports.finance_entries WHERE tenant_id=? AND day<=? GROUP BY account_id",RowMapper { r,_->id(r,"account_id") to r.getBigDecimal("balance") },tenant,date(until)).toMap()
 override fun append(tenant:UUID,entry:FinanceEntry){val e=entry;jdbc.update("INSERT INTO sports.finance_entries(tenant_id,id,document_id,account_id,day,kind,amount,actor_id,note,receipt_id,relates_to_month) VALUES(?,?,?,?,?,?,?,?,?,?,?)",tenant,e.id,e.documentId,e.accountId,date(e.day),e.kind,e.amount,e.actorId,e.note,e.receiptId,e.relatesToMonth)}
 private val handoverMapper=RowMapper { r:ResultSet,_:Int->FinanceHandover(id(r,"id"),id(r,"sender_id"),id(r,"receiver_id"),r.getBigDecimal("expected"),r.getString("status"),r.getString("requested_day"),r.getBigDecimal("received"),r.getString("accepted_day"),r.getString("note")) }
 override fun handovers(tenant:UUID)=jdbc.query("SELECT * FROM sports.finance_handovers WHERE tenant_id=? ORDER BY created_at DESC",handoverMapper,tenant)
 override fun saveHandover(tenant:UUID,value:FinanceHandover,dayIds:List<UUID>){val h=value;jdbc.update("INSERT INTO sports.finance_handovers(tenant_id,id,sender_id,receiver_id,expected,status,requested_day) VALUES(?,?,?,?,?,?,?)",tenant,h.id,h.senderId,h.receiverId,h.expected,h.status,date(h.requestedDay));dayIds.forEach{jdbc.update("INSERT INTO sports.finance_handover_days(tenant_id,handover_id,day_id) VALUES(?,?,?)",tenant,h.id,it)}}
 override fun acceptHandover(tenant:UUID,value:FinanceHandover){val h=value;checkSports(jdbc.update("UPDATE sports.finance_handovers SET status='ACCEPTED',received=?,accepted_day=?,note=?,accepted_at=now() WHERE tenant_id=? AND id=? AND status='PENDING'",h.received,date(h.acceptedDay!!),h.note,tenant,h.id)==1,"ALREADY_ACCEPTED","La entrega ya fue aceptada",409)}
 override fun latestClosedMonth(tenant:UUID):String?=jdbc.query("SELECT max(month) FROM sports.finance_months WHERE tenant_id=?",RowMapper { r,_->r.getString(1) },tenant).firstOrNull()
 override fun month(tenant:UUID,month:String)=jdbc.query("SELECT report FROM sports.finance_months WHERE tenant_id=? AND month=?",RowMapper { r,_->json.readValue(r.getString(1),FinanceReport::class.java) },tenant,month).firstOrNull()
 override fun closeMonth(tenant:UUID,report:FinanceReport){jdbc.update("INSERT INTO sports.finance_months(tenant_id,month,actor_id,note,report) VALUES(?,?,?,?,?::jsonb)",tenant,report.month,report.closedBy,report.note,json.writeValueAsString(report))}
 override fun command(tenant:UUID,id:UUID)=jdbc.query("SELECT actor_id,payload,result FROM sports.finance_commands WHERE tenant_id=? AND id=?",RowMapper { r,_->FinanceCommand(id(r,"actor_id"),r.getString("payload"),json.readValue(r.getString("result"),FinanceResult::class.java)) },tenant,id).firstOrNull()
 override fun saveCommand(tenant:UUID,id:UUID,command:FinanceCommand){jdbc.update("INSERT INTO sports.finance_commands(tenant_id,id,actor_id,payload,result) VALUES(?,?,?,?,?::jsonb)",tenant,id,command.actorId,command.payload,json.writeValueAsString(command.result))}
 override fun receipt(tenant:UUID,id:UUID)=jdbc.query("SELECT * FROM sports.finance_receipts WHERE tenant_id=? AND id=?",RowMapper { r,_->FinanceReceipt(id(r,"id"),id(r,"actor_id"),r.getString("name"),r.getString("content_type"),r.getBytes("content")) },tenant,id).firstOrNull()
 override fun saveReceipt(tenant:UUID,receipt:FinanceReceipt){jdbc.update("INSERT INTO sports.finance_receipts(tenant_id,id,actor_id,name,content_type,content) VALUES(?,?,?,?,?,?)",tenant,receipt.id,receipt.actorId,receipt.name,receipt.contentType,receipt.content)}
}
