package com.creativecode.sports.investments

import com.creativecode.sports.*
import com.creativecode.sports.finance.FinancePerson
import java.math.BigDecimal
import java.time.*
import java.util.UUID

enum class InvestmentCategory { EQUIPMENT, IMPROVEMENT, REPAIR, OTHER }
data class Contribution(val adminId:UUID,val amount:BigDecimal)
data class InvestmentInput(val id:UUID,val day:String,val category:InvestmentCategory,val description:String,val amount:BigDecimal,val contributions:List<Contribution>)
data class Investment(val input:InvestmentInput,val actorId:UUID,val createdAt:Instant)
data class ContributorTotal(val adminId:UUID,val amount:BigDecimal)
data class InvestmentOverview(val today:String,val month:String?,val administrators:List<FinancePerson>,val records:List<Investment>,val total:BigDecimal,val byAdministrator:List<ContributorTotal>,val periodTotal:BigDecimal)
interface InvestmentStore {
 fun <T> transaction(fn:()->T):T
 fun lock(tenant:UUID)
 fun timezone(tenant:UUID):String
 fun people(tenant:UUID):List<FinancePerson>
 fun find(tenant:UUID,id:UUID):Investment?
 fun save(tenant:UUID,value:Investment)
 fun records(tenant:UUID,month:String?):List<Investment>
 fun totals(tenant:UUID):List<ContributorTotal>
}
/** External administrator contributions. This service has no cash or finance-ledger write port. */
class InvestmentService(private val store:InvestmentStore,private val clock:Clock=Clock.systemUTC()) {
 private fun today(a:Actor)=clock.instant().atZone(ZoneId.of(store.timezone(a.tenantId))).toLocalDate()
 private fun money(n:BigDecimal){checkSports(n>BigDecimal.ZERO && n<=BigDecimal("999999999999.99") && n.scale()<=2,"INVALID_AMOUNT","Indica un importe positivo en COP, con máximo dos decimales")}
 fun create(a:Actor,input:InvestmentInput):Investment {
  a.admin()
  money(input.amount)
  checkSports(input.description.isNotBlank() && input.description.length<=2000,"INVALID_DESCRIPTION","Escribe el concepto de la inversión (máximo 2000 caracteres)")
  val date=try{LocalDate.parse(input.day)}catch(e:DateTimeException){throw SportsError("INVALID_DATE","Fecha inválida")}
  checkSports(date.year in 1900..9998,"INVALID_DATE","Revisa el año de la inversión")
  checkSports(input.contributions.size in 1..50 && input.contributions.map{it.adminId}.distinct().size==input.contributions.size,"INVALID_CONTRIBUTIONS","Agrega entre uno y 50 administradores sin repetir personas")
  input.contributions.forEach{money(it.amount)}
  checkSports(input.contributions.fold(BigDecimal.ZERO){sum,c->sum+c.amount}.compareTo(input.amount)==0,"CONTRIBUTIONS_MISMATCH","La suma de los aportes debe coincidir con el total de la inversión")
  val normalized=input.copy(day=date.toString(),description=input.description.trim(),amount=input.amount.setScale(2),contributions=input.contributions.map{it.copy(amount=it.amount.setScale(2))}.sortedBy{it.adminId.toString()})
  return store.transaction {
   store.lock(a.tenantId)
   val old=store.find(a.tenantId,input.id)
   if(old!=null){checkSports(old.actorId==a.id && old.input==normalized,"IDEMPOTENCY_CONFLICT","Ese registro ya fue guardado con otros datos",409);old}
   else {
    checkSports(date<=today(a),"FUTURE_DATE","Registra inversiones ya realizadas, no fechas futuras")
    val people=store.people(a.tenantId)
    checkSports(normalized.contributions.all{c->people.any{it.id==c.adminId && it.role==Role.ADMIN && it.active}},"INVALID_CONTRIBUTOR","Todos los aportantes deben ser administradores activos de este negocio")
    Investment(normalized,a.id,clock.instant()).also{store.save(a.tenantId,it)}
   }
  }
 }
 fun overview(a:Actor,month:String?):InvestmentOverview {
  a.admin()
  val selected=month?.takeIf{it.isNotBlank()}?.let{try{YearMonth.parse(it).toString()}catch(e:DateTimeException){throw SportsError("INVALID_MONTH","Mes inválido")}}
  return store.transaction {
   // Same tenant lock as creation: records and cumulative amounts reflect one consistent result.
   store.lock(a.tenantId)
   val records=store.records(a.tenantId,selected)
   val totals=store.totals(a.tenantId).sortedWith(compareByDescending<ContributorTotal>{it.amount}.thenBy{it.adminId.toString()})
   InvestmentOverview(today(a).toString(),selected,store.people(a.tenantId).filter{it.role==Role.ADMIN},records,
    totals.fold(BigDecimal.ZERO){sum,c->sum+c.amount},totals,records.fold(BigDecimal.ZERO){sum,r->sum+r.input.amount})
  }
 }
}
