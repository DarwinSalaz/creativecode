package com.creativecode.sports.finance

import com.creativecode.sports.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.*
import java.util.UUID

private data class State(
 val people:MutableList<FinancePerson> = mutableListOf(),val accounts:MutableList<FinanceAccount> = mutableListOf(),
 var base:BigDecimal=BigDecimal("150000"),val days:MutableList<FinanceDay> = mutableListOf(),
 val entries:MutableList<FinanceEntry> = mutableListOf(),val handovers:MutableList<FinanceHandover> = mutableListOf(),
 val grouped:MutableSet<UUID> = mutableSetOf(),val months:MutableMap<String,FinanceReport> = mutableMapOf(),
 val commands:MutableMap<UUID,FinanceCommand> = mutableMapOf(),val receipts:MutableMap<UUID,FinanceReceipt> = mutableMapOf()
){fun snapshot()=copy(people=people.toMutableList(),accounts=accounts.toMutableList(),days=days.toMutableList(),entries=entries.toMutableList(),handovers=handovers.toMutableList(),grouped=grouped.toMutableSet(),months=months.toMutableMap(),commands=commands.toMutableMap(),receipts=receipts.toMutableMap())}
private class MemoryFinanceStore:FinanceStore {
 var states=mutableMapOf<UUID,State>()
 fun state(t:UUID)=states.getOrPut(t){State()}
 @Synchronized override fun <T> transaction(block:()->T):T {val before=states.mapValues{it.value.snapshot()}.toMutableMap();try{return block()}catch(e:Exception){states=before;throw e}}
 override fun lock(tenant:UUID){}
 override fun timezone(tenant:UUID)="America/Bogota"
 override fun people(tenant:UUID)=state(tenant).people.toList()
 override fun accounts(tenant:UUID)=state(tenant).accounts.toList()
 override fun saveAccount(tenant:UUID,account:FinanceAccount){state(tenant).accounts.add(account)}
 override fun base(tenant:UUID)=state(tenant).base
 override fun saveBase(tenant:UUID,amount:BigDecimal){state(tenant).base=amount}
 override fun days(tenant:UUID,from:String,to:String)=state(tenant).days.filter{it.day>=from&&it.day<=to}
 override fun pendingDays(tenant:UUID,actor:UUID)=state(tenant).days.filter{it.actorId==actor&&it.cash>BigDecimal.ZERO&&it.id !in state(tenant).grouped}
 override fun saveDay(tenant:UUID,day:FinanceDay){state(tenant).days.add(day)}
 override fun entries(tenant:UUID,from:String,to:String)=state(tenant).entries.filter{it.day>=from&&it.day<=to}
 override fun balances(tenant:UUID,until:String)=state(tenant).entries.filter{it.day<=until}.groupBy{it.accountId}.mapValues{it.value.fold(BigDecimal.ZERO){s,e->s+e.amount}}
 override fun append(tenant:UUID,entry:FinanceEntry){state(tenant).entries.add(entry)}
 override fun handovers(tenant:UUID)=state(tenant).handovers.toList()
 override fun saveHandover(tenant:UUID,value:FinanceHandover,dayIds:List<UUID>){state(tenant).handovers.add(value);state(tenant).grouped.addAll(dayIds)}
 override fun acceptHandover(tenant:UUID,value:FinanceHandover){val s=state(tenant);s.handovers[s.handovers.indexOfFirst{it.id==value.id}]=value}
 override fun latestClosedMonth(tenant:UUID)=state(tenant).months.keys.max()
 override fun month(tenant:UUID,month:String)=state(tenant).months[month]
 override fun closeMonth(tenant:UUID,report:FinanceReport){state(tenant).months[report.month]=report}
 override fun command(tenant:UUID,id:UUID)=state(tenant).commands[id]
 override fun saveCommand(tenant:UUID,id:UUID,command:FinanceCommand){state(tenant).commands[id]=command}
 override fun receipt(tenant:UUID,id:UUID)=state(tenant).receipts[id]
 override fun saveReceipt(tenant:UUID,receipt:FinanceReceipt){state(tenant).receipts[receipt.id]=receipt}
}
class FinanceServiceTest {
 private val t=UUID.randomUUID()
 private val employee=Actor(UUID.randomUUID(),t,"Empleado",Role.EMPLOYEE)
 private val admin=Actor(UUID.randomUUID(),t,"Administrador",Role.ADMIN)
 private val other=Actor(UUID.randomUUID(),t,"Otro administrador",Role.ADMIN)
 private val store=MemoryFinanceStore().apply{state(t).people.addAll(listOf(employee,admin,other).map{FinancePerson(it.id,it.name,it.role,true)})}
 private val service=FinanceService(store,jacksonObjectMapper().findAndRegisterModules(),Clock.fixed(Instant.parse("2026-11-05T17:00:00Z"),ZoneOffset.UTC))
 private fun id()=UUID.randomUUID()
 private fun n(v:Int)=BigDecimal(v)
 private fun day(date:String="2026-11-01",cash:Int=500000)=DayInput(id(),date,n(cash))
 private fun bank(owner:UUID?=null):UUID{val id=id();service.bank(admin,BankInput(id,"Cuenta",owner));return id}
 private fun cash(a:Actor=employee)=store.accounts(t).first{it.kind=="CASH"&&it.ownerId==a.id}.id
 private fun balance(account:UUID)=store.balances(t,"2026-11-05")[account]?:BigDecimal.ZERO
 private fun error(code:String,block:()->Unit){try{block();fail("Expected $code")}catch(e:SportsError){assertEquals(code,e.code)}}
 @Test fun `base excluded and bank incomes belong to selected holders`(){val b=bank(admin.id);service.closeDay(employee,day().copy(transfers=listOf(FinanceTransfer(b,n(200000)))));assertEquals(n(500000),balance(cash()));assertEquals(n(200000),balance(b));assertEquals(n(150000),store.days(t,"2026-11-01","2026-11-05").first().cashBase);assertEquals(n(700000),service.report(admin,"2026-11").result)}
 @Test fun `retries do not duplicate and a day is unique across users`(){val d=day();val first=service.closeDay(employee,d);assertEquals(first,service.closeDay(employee,d));assertEquals(1,store.state(t).entries.size);error("IDEMPOTENCY_CONFLICT"){service.closeDay(employee,d.copy(cash=n(3)))};error("IDEMPOTENCY_CONFLICT"){service.closeDay(admin,d)};error("DAY_CLOSED"){service.closeDay(admin,day())}}
 @Test fun `multiple days transfer net of expenses and shortage is recorded once`(){service.closeDay(employee,day(cash=300000));service.closeDay(employee,day("2026-11-02",200000));service.expense(employee,ExpenseInput(id(),"2026-11-02",cash(),n(50000),"Insumos"));val h=id();service.handover(employee,HandoverInput(h,admin.id));assertEquals(n(450000),store.handovers(t).first().expected);assertEquals(n(450000),balance(cash()));val accept=AcceptInput(id(),n(440000),"Faltan diez mil al contar");service.accept(admin,h,accept);service.accept(admin,h,accept);assertEquals(n(0),balance(cash()));assertEquals(n(440000),balance(cash(admin)));val r=service.report(admin,"2026-11");assertEquals(n(500000),r.incomeCash);assertEquals(n(10000),r.shortages);assertEquals(n(440000),r.result);assertTrue(store.pendingDays(t,employee.id).isEmpty())}
 @Test fun `only recipient accepts and shortages require explanation`(){service.closeDay(employee,day());val h=id();service.handover(employee,HandoverInput(h,admin.id));error("FORBIDDEN"){service.accept(other,h,AcceptInput(id(),n(500000)))};error("NOTE_REQUIRED"){service.accept(admin,h,AcceptInput(id(),n(499999)))};assertEquals("PENDING",store.handovers(t).first().status);assertEquals(n(500000),balance(cash()))}
 @Test fun `pending handover locks outgoing cash but permits new daily income`(){service.closeDay(employee,day());val h=id();service.handover(employee,HandoverInput(h,admin.id));error("HANDOVER_PENDING"){service.expense(employee,ExpenseInput(id(),"2026-11-01",cash(),n(1),"Test"))};error("HANDOVER_PENDING"){service.handover(employee,HandoverInput(id(),other.id))};service.closeDay(employee,day("2026-11-02",100000));service.accept(admin,h,AcceptInput(id(),n(500000)));assertEquals(n(100000),balance(cash()));assertEquals(1,store.pendingDays(t,employee.id).size)}
 @Test fun `employee expenses require own or business account and positive available funds`(){val business=bank();val adminBank=bank(admin.id);service.closeDay(employee,day().copy(transfers=listOf(FinanceTransfer(business,n(200)),FinanceTransfer(adminBank,n(200)))));service.expense(employee,ExpenseInput(id(),"2026-11-01",business,n(100),"Compra"));error("FORBIDDEN"){service.expense(employee,ExpenseInput(id(),"2026-11-01",adminBank,n(100),"Compra"))};error("INSUFFICIENT_FUNDS"){service.expense(employee,ExpenseInput(id(),"2026-11-01",business,n(101),"Compra"))};error("INVALID_AMOUNT"){service.expense(employee,ExpenseInput(id(),"2026-11-01",business,n(-1),"Compra"))}}
 @Test fun `backdated expense cannot consume funds already handed over`(){service.closeDay(employee,day());val h=id();service.handover(employee,HandoverInput(h,admin.id));service.accept(admin,h,AcceptInput(id(),n(500000)));error("INSUFFICIENT_FUNDS"){service.expense(employee,ExpenseInput(id(),"2026-11-01",cash(),n(1),"Compra antigua"))}}
 @Test fun `closed reports stay immutable and prevent earlier backdating`(){service.closeDay(employee,day("2026-10-31",500000));service.closeMonth(admin,MonthInput(id(),"2026-10","Cuadre revisado"));val before=service.report(admin,"2026-10");error("MONTH_CLOSED"){service.closeDay(employee,day("2026-10-30"))};error("MONTH_CLOSED"){service.closeDay(employee,day("2026-09-01"))};error("MONTH_CLOSED"){service.expense(employee,ExpenseInput(id(),"2026-10-31",cash(),n(1),"Compra"))};service.adjustment(admin,AdjustmentInput(id(),"2026-11-05",cash(),n(-10000),"Corrección documentada","2026-10"));assertEquals(before,service.report(admin,"2026-10"));assertEquals(n(490000),balance(cash()));assertEquals(n(-10000),service.report(admin,"2026-11").adjustments)}
 @Test fun `month closing and settings require admin and ended month`(){error("FORBIDDEN"){service.closeMonth(employee,MonthInput(id(),"2026-10","Cierre"))};error("MONTH_IN_PROGRESS"){service.closeMonth(admin,MonthInput(id(),"2026-11","Cierre"))};error("FORBIDDEN"){service.bank(employee,BankInput(id(),"Cuenta"))};error("FUTURE_DATE"){service.closeDay(employee,day("2026-11-06"))}}
 @Test fun `cross tenant account cannot receive transfers or expenses`(){val b=bank();val alien=employee.copy(tenantId=id());error("ACCOUNT_NOT_FOUND"){service.closeDay(alien,day().copy(transfers=listOf(FinanceTransfer(b,n(100)))))};assertTrue(store.state(alien.tenantId).days.isEmpty());assertTrue(store.state(t).entries.isEmpty())}
 @Test fun `zero cash after expenses can be settled and new base is not retroactive`(){service.closeDay(employee,day(cash=100));service.base(admin,BaseInput(id(),n(200000)));service.expense(employee,ExpenseInput(id(),"2026-11-01",cash(),n(100),"Insumos"));val h=id();service.handover(employee,HandoverInput(h,admin.id));service.accept(admin,h,AcceptInput(id(),n(0)));assertEquals(n(150000),store.state(t).days.first().cashBase);assertEquals(n(200000),store.base(t));assertEquals(n(0),service.report(admin,"2026-11").result)}
 @Test fun `invalid transfer rolls back complete closing`(){val b=bank();error("INVALID_AMOUNT"){service.closeDay(employee,day().copy(transfers=listOf(FinanceTransfer(b,n(10)),FinanceTransfer(b,n(-1)))))};assertTrue(store.state(t).days.isEmpty());assertTrue(store.state(t).entries.isEmpty())}
 @Test fun `receipt type size and tenant are validated`(){error("INVALID_RECEIPT"){service.upload(employee,"fake.pdf","application/pdf","hello".toByteArray())};val id=service.upload(employee,"receipt.pdf","application/pdf","%PDF-1.4".toByteArray());assertEquals(employee.id,service.receipt(admin,id).actorId);error("NOT_FOUND"){service.receipt(employee.copy(tenantId=id()),id)}}
}
