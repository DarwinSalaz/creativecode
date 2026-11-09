package com.creativecode.sports.finance

import com.creativecode.sports.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class FinancePerson(val id: UUID,val name: String,val role: Role,val active: Boolean)
data class FinanceAccount(val id: UUID=UUID.randomUUID(),val name: String,val kind: String,val ownerId: UUID?=null)
data class FinanceTransfer(val accountId: UUID,val amount: BigDecimal)
data class FinanceDay(val id: UUID,val day: String,val actorId: UUID,val cash: BigDecimal,val cashBase: BigDecimal,val transfers: List<FinanceTransfer>,val note: String?=null)
data class FinanceEntry(val id: UUID=UUID.randomUUID(),val documentId: UUID,val accountId: UUID,val day: String,val kind: String,val amount: BigDecimal,val actorId: UUID,val note: String?=null,val receiptId: UUID?=null,val relatesToMonth: String?=null)
data class FinanceHandover(val id: UUID,val senderId: UUID,val receiverId: UUID,val expected: BigDecimal,val status: String="PENDING",val requestedDay: String,val received: BigDecimal?=null,val acceptedDay: String?=null,val note: String?=null)
data class FinanceResult(val id: UUID,val message: String)
data class FinanceCommand(val actorId: UUID,val payload: String,val result: FinanceResult)
data class FinanceBalance(val account: FinanceAccount,val balance: BigDecimal)
data class FinanceReport(val month: String,val incomeCash: BigDecimal,val incomeTransfer: BigDecimal,val expenses: BigDecimal,val shortages: BigDecimal,val adjustments: BigDecimal,val result: BigDecimal,val balances: List<FinanceBalance>,val days: List<FinanceDay>,val entries: List<FinanceEntry>,val handovers: List<FinanceHandover>,val closed: Boolean=false,val closedBy: UUID?=null,val closedAt: Instant?=null,val note: String?=null)
data class FinanceMovement(val entry:FinanceEntry,val balanceAfter:BigDecimal,val sourceDays:List<FinanceDay>,val handover:FinanceHandover?=null)
data class FinanceStatement(val account:FinanceAccount,val month:String,val opening:BigDecimal,val incoming:BigDecimal,val outgoing:BigDecimal,val closing:BigDecimal,val movements:List<FinanceMovement>)
data class FinanceReceipt(val id: UUID,val actorId: UUID,val name: String,val contentType: String,val content: ByteArray)
data class DayInput(val id: UUID,val day: String,val cash: BigDecimal,val transfers: List<FinanceTransfer> = emptyList(),val note: String?=null)
data class BankInput(val id: UUID,val name: String,val ownerId: UUID?=null)
data class BaseInput(val id: UUID,val amount: BigDecimal)
data class HandoverInput(val id: UUID,val receiverId: UUID)
data class AcceptInput(val id: UUID,val received: BigDecimal,val note: String?=null)
data class ExpenseInput(val id: UUID,val day: String,val accountId: UUID,val amount: BigDecimal,val note: String,val receiptId: UUID?=null)
data class MonthInput(val id: UUID,val month: String,val note: String)
data class AdjustmentInput(val id: UUID,val day: String,val accountId: UUID,val amount: BigDecimal,val note: String,val relatesToMonth: String)

/** Independent finance persistence port; never references the legacy sales domain. */
interface FinanceStore {
 fun <T> transaction(block:()->T):T
 fun lock(tenant:UUID)
 fun timezone(tenant:UUID):String
 fun people(tenant:UUID):List<FinancePerson>
 fun accounts(tenant:UUID):List<FinanceAccount>
 fun saveAccount(tenant:UUID,account:FinanceAccount)
 fun base(tenant:UUID):BigDecimal
 fun saveBase(tenant:UUID,amount:BigDecimal)
 fun days(tenant:UUID,from:String,to:String):List<FinanceDay>
 fun documentDays(tenant:UUID,documents:Set<UUID>):Map<UUID,List<FinanceDay>>
 fun pendingDays(tenant:UUID,actor:UUID):List<FinanceDay>
 fun saveDay(tenant:UUID,day:FinanceDay)
 fun entries(tenant:UUID,from:String,to:String):List<FinanceEntry>
 fun balances(tenant:UUID,until:String):Map<UUID,BigDecimal>
 fun append(tenant:UUID,entry:FinanceEntry)
 fun handovers(tenant:UUID):List<FinanceHandover>
 fun saveHandover(tenant:UUID,value:FinanceHandover,dayIds:List<UUID>)
 fun acceptHandover(tenant:UUID,value:FinanceHandover)
 fun latestClosedMonth(tenant:UUID):String?
 fun month(tenant:UUID,month:String):FinanceReport?
 fun closeMonth(tenant:UUID,report:FinanceReport)
 fun command(tenant:UUID,id:UUID):FinanceCommand?
 fun saveCommand(tenant:UUID,id:UUID,command:FinanceCommand)
 fun receipt(tenant:UUID,id:UUID):FinanceReceipt?
 fun saveReceipt(tenant:UUID,receipt:FinanceReceipt)
}
