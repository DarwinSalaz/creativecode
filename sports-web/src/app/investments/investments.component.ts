import {Component,Input,OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Actor} from '../reservations/models';
import {ApiService} from '../auth/api.service';
interface Administrator {id:string;name:string;active:boolean}
interface Contribution {adminId:string;amount:number}
interface Investment {input:{id:string;day:string;category:string;description:string;amount:number;contributions:Contribution[]};actorId:string;createdAt:string}
interface Overview {today:string;month?:string;administrators:Administrator[];records:Investment[];total:number;byAdministrator:Contribution[];periodTotal:number}
@Component({selector:'sports-investments',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./investments.component.html',styleUrls:['./investments.component.css']})
export class InvestmentsComponent implements OnInit {
 @Input() actor!:Actor;
 data?:Overview; month=''; busy=false; error=''; notice=''; editor=false;
 categories=[['EQUIPMENT','Equipos'],['IMPROVEMENT','Mejoras'],['REPAIR','Arreglos'],['OTHER','Otros']];
 form={day:'',category:'EQUIPMENT',description:'',amount:0,contributions:[] as Contribution[]};
 constructor(private api:ApiService){}
 ngOnInit(){if(this.actor.role==='ADMIN')void this.run(async()=>{await this.load();this.month=this.data!.today.slice(0,7);await this.load();});}
 async run(fn:()=>Promise<void>){if(this.busy)return;this.busy=true;this.error='';try{await fn();}catch(e:any){this.error=e.message||'No pudimos completar la operación';}finally{this.busy=false;}}
 async load(){this.data=await this.api.request<Overview>('/investments'+(this.month?'?month='+encodeURIComponent(this.month):''));}
 refresh(){void this.run(()=>this.load());}
 allMonths(){this.month='';this.refresh();}
 person(id:string){return this.data?.administrators.find(a=>a.id===id)?.name||'Administrador';}
 category(value:string){return this.categories.find(c=>c[0]===value)?.[1]||value;}
 date(value:string){return new Intl.DateTimeFormat('es-CO',{day:'numeric',month:'short',year:'numeric',timeZone:'UTC'}).format(new Date(value+'T12:00:00Z'));}
 active(){return this.data?.administrators.filter(a=>a.active)||[];}
 totals(){return (this.data?.administrators||[]).map(a=>({...a,amount:this.data?.byAdministrator.find(c=>c.adminId===a.id)?.amount||0})).sort((a,b)=>b.amount-a.amount||a.name.localeCompare(b.name));}
 percent(amount:number){return this.data?.total?100*amount/this.data.total:0;}
 open(){this.notice='';this.error='';this.form={day:this.data!.today,category:'EQUIPMENT',description:'',amount:0,contributions:[{adminId:this.actor.id,amount:0}]};this.editor=true;}
 syncSingle(){if(this.form.contributions.length===1)this.form.contributions[0].amount=this.form.amount;}
 add(){const next=this.active().find(a=>!this.form.contributions.some(c=>c.adminId===a.id));if(next)this.form.contributions.push({adminId:next.id,amount:0});}
 allocated(){return this.form.contributions.reduce((sum,c)=>sum+Math.round(Number(c.amount||0)*100),0)/100;}
 difference(){return (Math.round(Number(this.form.amount||0)*100)-Math.round(this.allocated()*100))/100;}
 valid(){return this.form.amount>0&&this.difference()===0&&this.form.contributions.every(c=>c.adminId&&c.amount>0)&&new Set(this.form.contributions.map(c=>c.adminId)).size===this.form.contributions.length;}
 save(){void this.run(async()=>{
  const payload=JSON.stringify(this.form),key='sports.investment.command.'+this.actor.tenantId+'.'+this.actor.id;
  let previous:any;try{previous=JSON.parse(sessionStorage.getItem(key)||'null');}catch{}
  const command=previous?.payload===payload?previous:{payload,id:crypto.randomUUID()};sessionStorage.setItem(key,JSON.stringify(command));
  await this.api.request('/investments','POST',{...this.form,id:command.id});sessionStorage.removeItem(key);
  this.month=this.form.day.slice(0,7);this.editor=false;this.notice='Inversión registrada. Los saldos de caja no cambian.';await this.load();
 });}
}
