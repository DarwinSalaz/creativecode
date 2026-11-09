import {Component,Input,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../auth/api.service';
import {Actor} from '../reservations/models';
interface Account {id:string;name:string;kind:string;ownerId?:string}
interface Balance {account:Account;balance:number}
interface Person {id:string;name:string;role:string;active:boolean}
interface Handover {id:string;senderId:string;receiverId:string;expected:number;received?:number;status:string;requestedDay:string;acceptedDay?:string;note?:string}
interface Movement {entry:{id:string;day:string;kind:string;amount:number;actorId:string;note?:string;receiptId?:string;relatesToMonth?:string};balanceAfter:number;sourceDays:{id:string;day:string;actorId:string;cash:number}[];handover?:Handover}
interface Statement {account:Account;month:string;opening:number;incoming:number;outgoing:number;closing:number;movements:Movement[]}
interface Overview {today:string;cashBase:number;people:Person[];accounts:Balance[];pendingDays:any[];handovers:Handover[]}
@Component({selector:'sports-finance',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./finance.component.html',styleUrls:['./finance.component.css']})
export class FinanceComponent implements OnInit {
 @Input() actor!:Actor;
 statement?:Statement; statementMonth=''; statementAccount=''; balanceScope='mine'; transferOpen=false;
 overview?:Overview; report:any; section='daily'; busy=false; error=''; notice=''; month=''; file?:File;
 day={day:'',cash:0,transfers:[] as {accountId:string;amount:number}[],note:''};
 expense={day:'',accountId:'',amount:0,note:'',receiptId:undefined as string|undefined};
 bank={name:'',ownerId:''}; base=150000; receiverId=''; accepting?:Handover; received=0; acceptNote=''; monthNote=''; monthConfirmed=false;
 adjustment={day:'',accountId:'',amount:0,note:'',relatesToMonth:''};
 private commands=new Map<string,{payload:string;id:string}>();
 constructor(public api:ApiService){}
 ngOnInit(){if(this.actor.role==='ADMIN')this.section='custody';void this.run(async()=>{await this.load();await this.loadReport();});}
 async run(fn:()=>Promise<void>){if(this.busy)return;this.busy=true;this.error='';this.notice='';try{await fn();}catch(e:any){this.error=e.message||'No pudimos completar la operación';}finally{this.busy=false;}}
 async load(){this.overview=await this.api.request<Overview>('/finance');this.base=this.overview.cashBase;for(const f of [this.day,this.expense,this.adjustment])if(!f.day)f.day=this.overview.today;if(!this.month)this.month=this.overview.today.slice(0,7);if(!this.statementMonth)this.statementMonth=this.overview.today.slice(0,7);await this.loadStatement();}
 async loadReport(){this.report=undefined;this.report=await this.api.request('/finance/report?month='+encodeURIComponent(this.month));this.monthConfirmed=false;}
 refresh(){void this.run(async()=>{await this.load();await this.loadReport();});}
 async post(path:string,body:any){
  // Retain the same command after an ambiguous network result, including component recreation.
  const payload=JSON.stringify(body),key='sports.finance.command.'+this.actor.tenantId+'.'+this.actor.id+'.'+path;
  let previous=this.commands.get(key);
  if(!previous){try{previous=JSON.parse(sessionStorage.getItem(key)||'null');}catch{}}
  const command=previous?.payload===payload?previous:{payload,id:crypto.randomUUID()};
  this.commands.set(key,command);sessionStorage.setItem(key,JSON.stringify(command));
  const result=await this.api.request<{message:string}>('/finance'+path,'POST',{...body,id:command.id});
  this.commands.delete(key);sessionStorage.removeItem(key);this.notice=result.message;
 }
 saveDay(){void this.run(async()=>{await this.post('/days',this.day);this.day={...this.day,cash:0,transfers:[],note:''};await this.load();await this.loadReport();});}
 addTransfer(){this.day.transfers.push({accountId:this.banks()[0]?.account.id||'',amount:0});}
 saveBank(){void this.run(async()=>{await this.post('/accounts',{name:this.bank.name,ownerId:this.bank.ownerId||null});this.bank={name:'',ownerId:''};await this.load();});}
 saveBase(){void this.run(async()=>{await this.post('/base',{amount:this.base});await this.load();});}
 deliver(){void this.run(async()=>{await this.post('/handovers',{receiverId:this.receiverId});this.transferOpen=false;await this.load();await this.loadReport();});}
 openAcceptance(h:Handover){this.accepting=h;this.received=h.expected;this.acceptNote='';}
 accept(){if(!this.accepting)return;const id=this.accepting.id;void this.run(async()=>{await this.post('/handovers/'+id+'/accept',{received:this.received,note:this.acceptNote});this.accepting=undefined;await this.load();await this.loadReport();});}
 selectFile(event:Event){this.file=(event.target as HTMLInputElement).files?.[0];this.expense.receiptId=undefined;this.error='';if(this.file&&this.file.size>2097152){this.error='El comprobante debe pesar máximo 2 MB';this.file=undefined;(event.target as HTMLInputElement).value='';}}
 async upload(){if(!this.file||this.expense.receiptId)return;const data=new FormData();data.append('file',this.file);const response=await fetch(this.api.base+'/finance/receipts',{method:'POST',headers:{Authorization:'Bearer '+this.api.token},body:data,credentials:'omit'});const result=await response.json();if(!response.ok)throw new Error(result.message||'No se pudo guardar el comprobante');this.expense.receiptId=result.id;}
 saveExpense(){void this.run(async()=>{await this.upload();await this.post('/expenses',this.expense);this.expense={...this.expense,amount:0,note:'',receiptId:undefined};this.file=undefined;const input=document.getElementById('finance-receipt') as HTMLInputElement|null;if(input)input.value='';await this.load();await this.loadReport();});}
 download(id:string){void this.run(async()=>{const response=await fetch(this.api.base+'/finance/receipts/'+id,{headers:{Authorization:'Bearer '+this.api.token},credentials:'omit',cache:'no-store'});if(!response.ok)throw new Error('No se pudo descargar el comprobante');const blob=await response.blob();const url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download='comprobante.'+(blob.type==='application/pdf'?'pdf':blob.type==='image/png'?'png':'jpg');link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});}
 closeMonth(){if(!this.monthConfirmed)return;void this.run(async()=>{await this.post('/months',{month:this.month,note:this.monthNote});this.monthNote='';await this.loadReport();});}
 saveAdjustment(){void this.run(async()=>{await this.post('/adjustments',this.adjustment);this.adjustment={...this.adjustment,amount:0,note:''};await this.load();await this.loadReport();});}
 visibleBalances(){return (this.overview?.accounts||[]).filter(b=>this.balanceScope==='all'||b.account.ownerId===this.actor.id).sort((a,b)=>Number(b.account.kind==='CASH')-Number(a.account.kind==='CASH'));}
 async loadStatement(){
  const visible=this.visibleBalances();if(!visible.some(b=>b.account.id===this.statementAccount))this.statementAccount=visible[0]?.account.id||'';
  this.statement=undefined;if(!this.statementAccount)return;
  this.statement=await this.api.request<Statement>('/finance/accounts/'+this.statementAccount+'/statement?month='+encodeURIComponent(this.statementMonth));
 }
 selectBalance(id:string){void this.run(async()=>{this.statementAccount=id;await this.loadStatement();});}
 scope(value:string){void this.run(async()=>{this.balanceScope=value;await this.loadStatement();});}
 openMyMoney(){this.section='custody';void this.run(async()=>{this.balanceScope='mine';this.statementAccount=this.overview?.accounts.find(b=>b.account.ownerId===this.actor.id&&b.account.kind==='CASH')?.account.id||'';await this.loadStatement();});}
 dateLabel(day:string){return new Intl.DateTimeFormat('es-CO',{day:'numeric',month:'short',year:'numeric',timeZone:'UTC'}).format(new Date(day+'T12:00:00Z'));}
 movementTitle(m:Movement){
  if(m.entry.kind==='INCOME')return this.statement?.account.kind==='BANK'?'Transferencia registrada en cierre diario':'Ingreso de cierre diario';
  if(m.entry.kind==='HANDOVER_IN')return 'Efectivo recibido de '+this.person(m.handover?.senderId);
  if(m.entry.kind==='HANDOVER_OUT')return 'Efectivo trasladado a '+this.person(m.handover?.receiverId);
  return this.label(m.entry.kind);
 }
 person(id?:string){return id?this.overview?.people.find(p=>p.id===id)?.name||'Usuario':'Negocio';}
 account(id:string){return this.overview?.accounts.find(b=>b.account.id===id)?.account.name||'Cuenta';}
 banks(){return this.overview?.accounts.filter(b=>b.account.kind==='BANK')||[];}
 expenseAccounts(){return this.overview?.accounts.filter(b=>this.actor.role==='ADMIN'||b.account.ownerId===this.actor.id||!b.account.ownerId)||[];}
 admins(){return this.overview?.people.filter(p=>p.active&&p.role==='ADMIN'&&p.id!==this.actor.id)||[];}
 myCash(){return this.overview?.accounts.find(b=>b.account.kind==='CASH'&&b.account.ownerId===this.actor.id)?.balance||0;}
 myPending(){return this.overview?.handovers.some(h=>h.senderId===this.actor.id&&h.status==='PENDING');}
 pending(){return this.overview?.handovers.filter(h=>h.status==='PENDING')||[];}
 totalDay(){return Number(this.day.cash||0)+this.day.transfers.reduce((s,t)=>s+Number(t.amount||0),0);}
 label(kind:string){return ({INCOME:'Ingreso',EXPENSE:'Gasto',HANDOVER_IN:'Efectivo recibido',HANDOVER_OUT:'Efectivo entregado',SHORTAGE:'Faltante',ADJUSTMENT:'Ajuste'} as Record<string,string>)[kind]||kind;}
}
