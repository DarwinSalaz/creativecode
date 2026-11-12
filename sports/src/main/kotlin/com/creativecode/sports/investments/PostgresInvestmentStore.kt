package com.creativecode.sports.investments

import com.creativecode.sports.*
import com.creativecode.sports.finance.FinancePerson
import org.springframework.jdbc.core.RowMapper
import java.sql.Date
import java.sql.Timestamp
import java.time.YearMonth
import java.util.UUID

class PostgresInvestmentStore(private val sports:PostgresSportsStore):InvestmentStore {
 private val jdbc=sports.jdbc
 override fun <T> transaction(fn:()->T):T=sports.transaction(fn)
 override fun lock(tenant:UUID){checkSports(jdbc.queryForList("SELECT id FROM sports.tenants WHERE id=? FOR UPDATE",tenant).size==1,"TENANT_NOT_FOUND","Negocio no disponible",403)}
 override fun timezone(tenant:UUID)=sports.settings(tenant).timezone
 override fun people(tenant:UUID)=jdbc.query("SELECT id,name,role,active FROM sports.users WHERE tenant_id=? ORDER BY name",RowMapper{r,_->FinancePerson(r.getObject("id") as UUID,r.getString("name"),Role.valueOf(r.getString("role")),r.getBoolean("active"))},tenant)
 private fun read(tenant:UUID,condition:String,args:List<Any>):List<Investment> {
  val documents=jdbc.query("SELECT id,day,category,description,amount,actor_id,created_at FROM sports.investments i WHERE i.tenant_id=? $condition ORDER BY i.day DESC,i.created_at DESC,i.id DESC",RowMapper{r,_->Investment(InvestmentInput(r.getObject("id") as UUID,r.getString("day"),InvestmentCategory.valueOf(r.getString("category")),r.getString("description"),r.getBigDecimal("amount"),emptyList()),r.getObject("actor_id") as UUID,r.getTimestamp("created_at").toInstant())},*(listOf(tenant)+args).toTypedArray())
  if(documents.isEmpty())return documents
  val parts=jdbc.query("SELECT c.investment_id,c.admin_id,c.amount FROM sports.investment_contributions c JOIN sports.investments i ON i.tenant_id=c.tenant_id AND i.id=c.investment_id WHERE i.tenant_id=? "+condition+" ORDER BY c.admin_id",RowMapper{r,_->(r.getObject("investment_id") as UUID) to Contribution(r.getObject("admin_id") as UUID,r.getBigDecimal("amount"))},*(listOf(tenant)+args).toTypedArray()).groupBy({it.first},{it.second})
  return documents.map{it.copy(input=it.input.copy(contributions=parts[it.input.id]?:emptyList()))}
 }
 override fun find(tenant:UUID,id:UUID)=read(tenant," AND i.id=?",listOf(id)).firstOrNull()
 override fun records(tenant:UUID,month:String?):List<Investment> = if(month==null)read(tenant,"",emptyList()) else YearMonth.parse(month).let{read(tenant," AND i.day BETWEEN ? AND ?",listOf(Date.valueOf(it.atDay(1)),Date.valueOf(it.atEndOfMonth())))}
 override fun totals(tenant:UUID)=jdbc.query("SELECT admin_id,sum(amount) AS total FROM sports.investment_contributions WHERE tenant_id=? GROUP BY admin_id",RowMapper{r,_->ContributorTotal(r.getObject("admin_id") as UUID,r.getBigDecimal("total"))},tenant)
 override fun save(tenant:UUID,value:Investment){val i=value.input
  jdbc.update("INSERT INTO sports.investments(tenant_id,id,day,category,description,amount,actor_id,created_at) VALUES(?,?,?,?,?,?,?,?)",tenant,i.id,Date.valueOf(i.day),i.category.name,i.description,i.amount,value.actorId,Timestamp.from(value.createdAt))
  i.contributions.forEach{jdbc.update("INSERT INTO sports.investment_contributions(tenant_id,investment_id,admin_id,amount) VALUES(?,?,?,?)",tenant,i.id,it.adminId,it.amount)}
  sports.audit(Actor(value.actorId,tenant,"",Role.ADMIN),i.id,"INVESTMENT_CREATED",null)
 }
}
