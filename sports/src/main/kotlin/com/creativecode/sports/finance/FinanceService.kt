package com.creativecode.sports.finance

import com.creativecode.sports.*
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.*
import java.util.UUID

class FinanceService(private val store:FinanceStore,private val json:ObjectMapper,private val clock:Clock=Clock.systemUTC()) {
 private val zero=BigDecimal.ZERO
 private fun today(a:Actor)=clock.instant().atZone(ZoneId.of(store.timezone(a.tenantId))).toLocalDate()
 private fun money(n:BigDecimal,positive:Boolean=false){checkSports(n.scale()<=2 && n.abs()<=BigDecimal("999999999999.99") && (if(positive)n>zero else n>=zero),"INVALID_AMOUNT","Usa un importe en COP válido con máximo dos decimales")}
 private fun note(s:String?,required:Boolean=false){checkSports((s?.length?:0)<=1000 && (!required||!s.isNullOrBlank()),"NOTE_REQUIRED","Escribe un concepto o nota (máximo 1000 caracteres)")}
 private fun openDay(a:Actor,value:String):LocalDate {val day=LocalDate.parse(value);checkSports(day<=today(a),"FUTURE_DATE","No puedes registrar fechas futuras");checkSports(store.latestClosedMonth(a.tenantId)?.let{YearMonth.from(day)>YearMonth.parse(it)}?:true,"MONTH_CLOSED","El mes está cerrado; registra un ajuste en un mes abierto",409);return day}
 private fun account(a:Actor,id:UUID)=store.accounts(a.tenantId).find{it.id==id}?:throw SportsError("ACCOUNT_NOT_FOUND","Cuenta no encontrada",404)
 private fun person(a:Actor,id:UUID)=store.people(a.tenantId).find{it.id==id&&it.active}?:throw SportsError("PERSON_NOT_FOUND","Persona no disponible",404)
 private fun cash(a:Actor,owner:UUID):FinanceAccount=store.accounts(a.tenantId).find{it.kind=="CASH"&&it.ownerId==owner}?:FinanceAccount(name="Efectivo · "+person(a,owner).name,kind="CASH",ownerId=owner).also{store.saveAccount(a.tenantId,it)}
 private fun balance(a:Actor,id:UUID,day:String=today(a).toString())=store.balances(a.tenantId,day)[id]?:zero
 private fun writable(a:Actor,account:FinanceAccount){checkSports(a.role==Role.ADMIN||account.ownerId==a.id||(account.kind=="BANK"&&account.ownerId==null),"FORBIDDEN","Solo puedes registrar gastos de tu efectivo, tus cuentas o cuentas del negocio",403);if(account.kind=="CASH")checkSports(store.handovers(a.tenantId).none{it.senderId==account.ownerId&&it.status=="PENDING"},"HANDOVER_PENDING","Ese efectivo tiene una entrega pendiente de aceptación",409)}
 private fun funds(a:Actor,id:UUID,amount:BigDecimal,day:String){
  var remaining=balance(a,id,day)-amount
  checkSports(remaining>=zero,"INSUFFICIENT_FUNDS","Saldo insuficiente en la fecha seleccionada",409)
  store.entries(a.tenantId,LocalDate.parse(day).plusDays(1).toString(),today(a).toString()).filter{it.accountId==id}.groupBy{it.day}.toSortedMap().forEach{_,entries->remaining+=entries.fold(zero){sum,e->sum+e.amount};checkSports(remaining>=zero,"INSUFFICIENT_FUNDS","El gasto dejaría sin saldo movimientos posteriores",409)}
 }
 private fun command(a:Actor,id:UUID,type:String,input:Any,action:()->String):FinanceResult=store.transaction{
  store.lock(a.tenantId)
  val payload=type+":"+json.writeValueAsString(input)
  val previous=store.command(a.tenantId,id)
  if(previous!=null){checkSports(previous.actorId==a.id&&previous.payload==payload,"IDEMPOTENCY_CONFLICT","La solicitud ya fue usada con otros datos",409);previous.result}
  else {val result=FinanceResult(id,action());store.saveCommand(a.tenantId,id,FinanceCommand(a.id,payload,result));result}
 }
 fun bank(a:Actor,input:BankInput)=command(a,input.id,"BANK",input){a.admin();checkSports(input.name.isNotBlank()&&input.name.length<=150,"INVALID_ACCOUNT","Indica el nombre de la cuenta");input.ownerId?.let{person(a,it)};store.saveAccount(a.tenantId,FinanceAccount(input.id,input.name.trim(),"BANK",input.ownerId));"Cuenta registrada"}
 fun base(a:Actor,input:BaseInput)=command(a,input.id,"BASE",input){a.admin();money(input.amount);store.saveBase(a.tenantId,input.amount);"Base de caja actualizada"}
 fun closeDay(a:Actor,input:DayInput)=command(a,input.id,"DAY",input){
  openDay(a,input.day);money(input.cash);note(input.note)
  checkSports(store.days(a.tenantId,input.day,input.day).isEmpty(),"DAY_CLOSED","Ya existe un cierre para este día y esta caja",409)
  checkSports(input.transfers.size<=30,"INVALID_TRANSFERS","Máximo 30 cuentas por cierre")
  input.transfers.forEach{money(it.amount,true);checkSports(account(a,it.accountId).kind=="BANK","INVALID_ACCOUNT","Las transferencias deben ir a una cuenta bancaria")}
  val day=FinanceDay(input.id,input.day,a.id,input.cash,store.base(a.tenantId),input.transfers,input.note)
  store.saveDay(a.tenantId,day)
  if(input.cash>zero)store.append(a.tenantId,FinanceEntry(documentId=input.id,accountId=cash(a,a.id).id,day=input.day,kind="INCOME",amount=input.cash,actorId=a.id,note=input.note))
  input.transfers.forEach{store.append(a.tenantId,FinanceEntry(documentId=input.id,accountId=it.accountId,day=input.day,kind="INCOME",amount=it.amount,actorId=a.id,note=input.note))}
  if(a.role==Role.ADMIN)"Cierre diario registrado. El efectivo se suma a tu saldo para el cuadre mensual; no requiere entrega." else "Cierre diario registrado. El efectivo sigue a tu cargo hasta que el administrador acepte la entrega."
 }
 fun handover(a:Actor,input:HandoverInput)=command(a,input.id,"HANDOVER",input){
  val day=today(a).toString();openDay(a,day)
  checkSports(input.receiverId!=a.id&&person(a,input.receiverId).role==Role.ADMIN,"INVALID_RECEIVER","Selecciona otro administrador como receptor")
  checkSports(store.handovers(a.tenantId).none{it.senderId==a.id&&it.status=="PENDING"},"HANDOVER_PENDING","Ya tienes una entrega pendiente",409)
  val days=store.pendingDays(a.tenantId,a.id);checkSports(a.role==Role.ADMIN||days.isNotEmpty(),"NO_PENDING_CASH","No tienes cierres con efectivo pendientes de entregar",409)
  val expected=balance(a,cash(a,a.id).id);checkSports(a.role!=Role.ADMIN||expected>zero,"NO_AVAILABLE_CASH","No tienes efectivo disponible para trasladar",409);checkSports(expected>=zero,"INVALID_BALANCE","Revisa el saldo antes de entregar",409)
  store.saveHandover(a.tenantId,FinanceHandover(input.id,a.id,input.receiverId,expected,requestedDay=day),days.map{it.id})
  if(a.role==Role.ADMIN)"Traslado solicitado; el dinero permanece a tu cargo hasta que el otro administrador lo acepte" else "Entrega completa solicitada; el dinero permanece a tu cargo hasta su aceptación"
 }
 fun accept(a:Actor,handoverId:UUID,input:AcceptInput)=command(a,input.id,"ACCEPT:$handoverId",input){
  a.admin();val day=today(a).toString();openDay(a,day)
  val h=store.handovers(a.tenantId).find{it.id==handoverId}?:throw SportsError("NOT_FOUND","Entrega no encontrada",404)
  checkSports(h.receiverId==a.id,"FORBIDDEN","Solo el administrador receptor puede aceptar",403)
  checkSports(h.status=="PENDING","ALREADY_ACCEPTED","La entrega ya fue aceptada",409)
  money(input.received);checkSports(input.received<=h.expected,"INVALID_RECEIVED","El importe recibido no puede superar el declarado")
  val shortage=h.expected-input.received;note(input.note,shortage>zero)
  val from=cash(a,h.senderId);funds(a,from.id,h.expected,day)
  if(input.received>zero){store.append(a.tenantId,FinanceEntry(documentId=h.id,accountId=from.id,day=day,kind="HANDOVER_OUT",amount=input.received.negate(),actorId=a.id,note=input.note));store.append(a.tenantId,FinanceEntry(documentId=h.id,accountId=cash(a,a.id).id,day=day,kind="HANDOVER_IN",amount=input.received,actorId=a.id,note=input.note))}
  if(shortage>zero)store.append(a.tenantId,FinanceEntry(documentId=h.id,accountId=from.id,day=day,kind="SHORTAGE",amount=shortage.negate(),actorId=a.id,note=input.note))
  store.acceptHandover(a.tenantId,h.copy(status="ACCEPTED",received=input.received,acceptedDay=day,note=input.note))
  if(shortage>zero)"Entrega aceptada con faltante registrado; no queda deuda al empleado" else "Entrega aceptada"
 }
 fun expense(a:Actor,input:ExpenseInput)=command(a,input.id,"EXPENSE",input){
  openDay(a,input.day);money(input.amount,true);note(input.note,true);val acc=account(a,input.accountId);writable(a,acc);funds(a,acc.id,input.amount,input.day)
  input.receiptId?.let{val r=store.receipt(a.tenantId,it)?:throw SportsError("NOT_FOUND","Comprobante no encontrado",404);checkSports(r.actorId==a.id||a.role==Role.ADMIN,"FORBIDDEN","Comprobante de otro usuario",403)}
  store.append(a.tenantId,FinanceEntry(documentId=input.id,accountId=acc.id,day=input.day,kind="EXPENSE",amount=input.amount.negate(),actorId=a.id,note=input.note.trim(),receiptId=input.receiptId));"Gasto registrado y descontado del saldo"
 }
 fun adjustment(a:Actor,input:AdjustmentInput)=command(a,input.id,"ADJUSTMENT",input){
  a.admin();openDay(a,input.day);money(input.amount.abs(),true);note(input.note,true)
  val related=YearMonth.parse(input.relatesToMonth).toString();checkSports(store.month(a.tenantId,related)!=null,"MONTH_NOT_CLOSED","Selecciona un mes cerrado para justificar la corrección")
  val acc=account(a,input.accountId);writable(a,acc);if(input.amount<zero)funds(a,acc.id,input.amount.negate(),input.day)
  store.append(a.tenantId,FinanceEntry(documentId=input.id,accountId=acc.id,day=input.day,kind="ADJUSTMENT",amount=input.amount,actorId=a.id,note=input.note.trim(),relatesToMonth=related));"Ajuste registrado en el mes abierto sin modificar el historial cerrado"
 }
 private fun buildReport(a:Actor,month:String):FinanceReport {
  val m=YearMonth.parse(month);val from=m.atDay(1).toString();val to=m.atEndOfMonth().toString();val accounts=store.accounts(a.tenantId);val entries=store.entries(a.tenantId,from,to)
  fun total(kind:String)=entries.filter{it.kind==kind}.fold(zero){s,e->s+e.amount}
  val cash=entries.filter{it.kind=="INCOME"&&accounts.any{acc->acc.id==it.accountId&&acc.kind=="CASH"}}.fold(zero){s,e->s+e.amount}
  val transfer=total("INCOME")-cash;val expense=total("EXPENSE").negate();val shortage=total("SHORTAGE").negate();val adjustments=total("ADJUSTMENT")
  val balances=store.balances(a.tenantId,to)
  return FinanceReport(month,cash,transfer,expense,shortage,adjustments,cash+transfer-expense-shortage+adjustments,accounts.map{FinanceBalance(it,balances[it.id]?:zero)},store.days(a.tenantId,from,to),entries,store.handovers(a.tenantId).filter{it.requestedDay<=to&&(it.acceptedDay==null||it.acceptedDay>=from)}.map{if(it.acceptedDay!=null&&it.acceptedDay>to)it.copy(status="PENDING",received=null,acceptedDay=null,note=null) else it})
 }
 fun report(a:Actor,month:String):FinanceReport=store.transaction{store.lock(a.tenantId);store.month(a.tenantId,YearMonth.parse(month).toString())?:buildReport(a,month)}
 /** Read-only projection of the existing ledger, including source closings from earlier months. */
 fun statement(a:Actor,accountId:UUID,month:String):FinanceStatement=store.transaction {
  store.lock(a.tenantId)
  val acc=account(a,accountId);val m=YearMonth.parse(month)
  val opening=store.balances(a.tenantId,m.atDay(1).minusDays(1).toString())[accountId]?:zero
  val entries=store.entries(a.tenantId,m.atDay(1).toString(),m.atEndOfMonth().toString()).filter{it.accountId==accountId}.sortedBy{it.day}
  val sources=store.documentDays(a.tenantId,entries.map{it.documentId}.toSet())
  val handovers=store.handovers(a.tenantId).associateBy{it.id}
  var running=opening
  val movements=entries.map{e->running+=e.amount;FinanceMovement(e,running,sources[e.documentId]?:emptyList(),handovers[e.documentId])}
  FinanceStatement(acc,m.toString(),opening,entries.filter{it.amount>zero}.fold(zero){s,e->s+e.amount},entries.filter{it.amount<zero}.fold(zero){s,e->s-e.amount},running,movements.reversed())
 }
 fun closeMonth(a:Actor,input:MonthInput)=command(a,input.id,"MONTH",input){
  a.admin();note(input.note,true);val month=YearMonth.parse(input.month)
  checkSports(month<YearMonth.from(today(a)),"MONTH_IN_PROGRESS","Solo puedes cerrar meses que ya terminaron")
  checkSports(store.latestClosedMonth(a.tenantId)?.let{month>YearMonth.parse(it)}?:true,"MONTH_CLOSED","El mes ya está cerrado",409)
  val report=buildReport(a,input.month).copy(closed=true,closedBy=a.id,closedAt=clock.instant(),note=input.note.trim())
  store.closeMonth(a.tenantId,report);"Mes cerrado. Las correcciones requieren un ajuste justificado en un mes abierto."
 }
 fun overview(a:Actor):Map<String,Any> = store.transaction{
  store.lock(a.tenantId);val balances=store.balances(a.tenantId,today(a).toString())
  mapOf("cashBase" to store.base(a.tenantId),"today" to today(a).toString(),"people" to store.people(a.tenantId),"accounts" to store.accounts(a.tenantId).map{FinanceBalance(it,balances[it.id]?:zero)},"pendingDays" to store.pendingDays(a.tenantId,a.id),"handovers" to store.handovers(a.tenantId))
 }
 fun upload(a:Actor,name:String,contentType:String,bytes:ByteArray):UUID=store.transaction{
  checkSports(bytes.isNotEmpty()&&bytes.size<=2097152,"INVALID_RECEIPT","Comprobante máximo de 2 MB")
  val signature=when(contentType){"application/pdf"->bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray());"image/png"->bytes.take(8).toByteArray().contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10));"image/jpeg"->bytes.size>2&&bytes[0]==(-1).toByte()&&bytes[1]==(-40).toByte();else->false}
  checkSports(signature,"INVALID_RECEIPT","Usa un archivo PDF, PNG o JPEG válido")
  val r=FinanceReceipt(UUID.randomUUID(),a.id,name.take(150).replace(Regex("[\\r\\n]"),""),contentType,bytes);store.saveReceipt(a.tenantId,r);r.id
 }
 fun receipt(a:Actor,id:UUID)=store.receipt(a.tenantId,id)?:throw SportsError("NOT_FOUND","Comprobante no encontrado",404)
}
