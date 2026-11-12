package com.creativecode.sports.investments

import com.creativecode.sports.*
import com.creativecode.sports.finance.FinancePerson
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.*
import java.util.UUID

private class MemoryInvestments:InvestmentStore {
 val users=mutableMapOf<UUID,List<FinancePerson>>()
 val data=mutableMapOf<Pair<UUID,UUID>,Investment>()
 @Synchronized override fun <T> transaction(fn:()->T):T=fn()
 override fun lock(tenant:UUID){}
 override fun timezone(tenant:UUID)="America/Bogota"
 override fun people(tenant:UUID)=users[tenant]?:emptyList()
 override fun find(tenant:UUID,id:UUID)=data[tenant to id]
 override fun save(tenant:UUID,value:Investment){data[tenant to value.input.id]=value}
 override fun records(tenant:UUID,month:String?)=data.filterKeys{it.first==tenant}.values.filter{month==null||it.input.day.startsWith(month)}
 override fun totals(tenant:UUID)=records(tenant,null).flatMap{it.input.contributions}.groupBy{it.adminId}.map{(id,parts)->ContributorTotal(id,parts.fold(BigDecimal.ZERO){s,c->s+c.amount})}
}
class InvestmentServiceTest {
 private val tenant=UUID.randomUUID()
 private val admin=Actor(UUID.randomUUID(),tenant,"Ana",Role.ADMIN)
 private val other=Actor(UUID.randomUUID(),tenant,"Luis",Role.ADMIN)
 private val employee=Actor(UUID.randomUUID(),tenant,"Empleado",Role.EMPLOYEE)
 private val store=MemoryInvestments().apply{users[tenant]=listOf(admin,other,employee).map{FinancePerson(it.id,it.name,it.role,true)}}
 private val service=InvestmentService(store,Clock.fixed(Instant.parse("2026-11-12T02:00:00Z"),ZoneOffset.UTC))
 private fun input()=InvestmentInput(UUID.randomUUID(),"2026-11-11",InvestmentCategory.REPAIR," Arreglo de malla ",BigDecimal("500000"),listOf(Contribution(admin.id,BigDecimal("200000")),Contribution(other.id,BigDecimal("300000"))))
 private fun error(code:String,block:()->Unit){try{block();fail("Expected $code")}catch(e:SportsError){assertEquals(code,e.code)}}
 @Test fun `shared investment records exact contributions and historical ranking across months`(){
  val saved=service.create(admin,input());assertEquals("Arreglo de malla",saved.input.description)
  service.create(other,input().copy(day="2026-10-01"))
  val result=service.overview(admin,"2026-11")
  assertEquals(BigDecimal("1000000.00"),result.total);assertEquals(BigDecimal("500000.00"),result.periodTotal)
  assertEquals(1,result.records.size);assertEquals(other.id,result.byAdministrator.first().adminId)
  assertEquals(BigDecimal("600000.00"),result.byAdministrator.first().amount)
  assertEquals(2,service.overview(admin,null).records.size)
 }
 @Test fun `employee cannot read or create investments`(){error("FORBIDDEN"){service.overview(employee,null)};error("FORBIDDEN"){service.create(employee,input())};assertTrue(store.data.isEmpty())}
 @Test fun `duplicate retry is stable despite ordering and numeric scale`(){val i=input();val saved=service.create(admin,i);assertEquals(saved,service.create(admin,i.copy(contributions=i.contributions.reversed(),amount=i.amount.setScale(2))));assertEquals(1,store.data.size);error("IDEMPOTENCY_CONFLICT"){service.create(other,i)};error("IDEMPOTENCY_CONFLICT"){service.create(admin,i.copy(description="Otro arreglo"))}}
 @Test fun `split must sum exactly and admins cannot repeat`(){val i=input();error("CONTRIBUTIONS_MISMATCH"){service.create(admin,i.copy(amount=BigDecimal("500001")))};error("INVALID_CONTRIBUTIONS"){service.create(admin,i.copy(contributions=i.contributions.map{it.copy(adminId=admin.id)}))};error("INVALID_CONTRIBUTIONS"){service.create(admin,i.copy(contributions=emptyList()))};assertTrue(store.data.isEmpty())}
 @Test fun `only active administrators of same tenant contribute`(){val i=input();for(id in listOf(employee.id,UUID.randomUUID())){error("INVALID_CONTRIBUTOR"){service.create(admin,i.copy(contributions=i.contributions.map{it.copy(adminId=if(it.adminId==admin.id)id else it.adminId)}))}};store.users[tenant]=store.people(tenant).map{it.copy(active=false)};error("INVALID_CONTRIBUTOR"){service.create(admin,i)};assertTrue(store.data.isEmpty())}
 @Test fun `other tenant has no access to records or totals`(){service.create(admin,input());val result=service.overview(admin.copy(tenantId=UUID.randomUUID()),null);assertTrue(result.records.isEmpty());assertEquals(0,result.total.signum())}
 @Test fun `invalid amounts dates and descriptions never persist`(){val i=input();for(n in listOf("0","-1","1.001","1000000000000")){error("INVALID_AMOUNT"){service.create(admin,i.copy(amount=BigDecimal(n)))}};error("INVALID_AMOUNT"){service.create(admin,i.copy(contributions=listOf(Contribution(admin.id,BigDecimal.ZERO))))};error("FUTURE_DATE"){service.create(admin,i.copy(day="2026-11-12"))};error("INVALID_DATE"){service.create(admin,i.copy(day="2026-02-30"))};error("INVALID_DESCRIPTION"){service.create(admin,i.copy(description=" "))};error("INVALID_MONTH"){service.overview(admin,"2026-13")};assertTrue(store.data.isEmpty())}
 @Test fun `historical records retain inactive contributor totals`(){service.create(admin,input());store.users[tenant]=store.people(tenant).map{it.copy(active=false)};assertEquals(2,service.overview(admin,null).administrators.size);assertEquals(BigDecimal("500000.00"),service.overview(admin,null).total)}
}
