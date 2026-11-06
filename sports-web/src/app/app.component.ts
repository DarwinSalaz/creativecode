import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonApp, IonContent } from '@ionic/angular';
import { FinanceComponent } from './finance/finance.component';
import { ApiService } from './auth/api.service';
import { Actor,Booking,Court,Contact,Settings,Operations,labels } from './reservations/models';
import {matchTiming,clockLabel,alertKeys} from './operations/timer.mjs';
import {availableRewardPercent,priceQuote,scheduledPrice,reservationQuote} from './reservations/price-quote.mjs';

@Component({selector:'sports-app',standalone:true,imports:[CommonModule,FormsModule,IonApp,IonContent,FinanceComponent],templateUrl:'./app.component.html',styleUrls:['./app.component.css']})
export class AppComponent implements OnInit,OnDestroy {
  slots=Array.from({length:24},(_,i)=>i*60);
  hours=Array.from({length:24},(_,i)=>i); clientSearch='';
  actor?:Actor; labels=labels; tab='calendar'; calendarMode='week'; selectedDate=''; courtFilter='';
  courts:Court[]=[]; bookings:Booking[]=[]; active:Booking[]=[]; upcoming:Booking[]=[]; clients:Contact[]=[]; teachers:Contact[]=[]; history:Booking[]=[];
  settings:Settings={name:'Cancha',timezone:'America/Bogota',currency:'COP',toleranceMinutes:15,waterMinutes:30,warningMinutes:5};
  error=''; notice=''; busy=false; refreshing=false; loading=false; lastSync=''; now=Date.now(); serverAnchor=Date.now(); performanceAnchor=performance.now();
  login={tenantId:localStorage.getItem('sports.business')||'',username:'',password:''};
  editor=false; editing?:Booking; form:any={}; contact:{id?:string;name:string;phone:string;notes:string}={name:'',phone:'',notes:''}; contactTeacher=false;
  courtForm:any={name:'',sport:'Fútbol',capacity:10,durationMinutes:60,color:'#167d69',active:true};
  venues:any[]=[]; venueForm:any={name:'',address:''};
  pricingEditor=false; pricingCourtFilter=''; weekdays=['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
  pricing:any={courtId:'',weekday:1,fromMinute:960,toMinute:1380,amount:0}; prices:any[]=[];
  recurrence:any={clientId:'',courtId:'',guestName:'',teamName:'',phone:'',weekday:1,localTime:'19:00',durationMinutes:60,fromDate:'',untilDate:''};
  userForm={username:'',name:'',password:'',role:'EMPLOYEE'}; users:any[]=[];
  loyalty:any[]=[]; loyaltyClient?:Contact; loyaltyForm:any={requiredMatches:5,rewardPercent:100,maxGapDays:21,active:true}; loyaltyVersion=0; loyaltyEvents:any[]=[]; exceptionDate=''; exceptionReason='';
  audio=false; private audioContext?:AudioContext; private alertNodes:OscillatorNode[]=[]; private alertGain?:GainNode; private interval:any; private refreshInterval:any;
  device=localStorage.getItem('sports.device')||crypto.randomUUID();
  private initialCalendarScroll=true;
  private sounded=new Set<string>(JSON.parse(sessionStorage.getItem('sports.alerts')||'[]'));
  constructor(public api:ApiService){localStorage.setItem('sports.device',this.device);this.selectedDate=this.dateKey(new Date());}
  copy<T>(value:T):T{return {...value};}
  dateKeyDate(value:string){return this.dateKey(new Date(value));}
  filteredClients(){return this.clients.filter(c=>c.name.toLowerCase().includes(this.clientSearch.toLowerCase()));}
  minuteTime(n:number){return `${Math.floor(n/60).toString().padStart(2,'0')}:${(n%60).toString().padStart(2,'0')}`;}
  timeMinute(s:string){const [h,m]=s.split(':').map(Number);return h*60+m;}
  eventTop(b:Booking){return this.timeMinute(this.time(b.start))*.8;}
  eventHeight(b:Booking){return Math.max(25,Math.min((Date.parse(b.end)-Date.parse(b.start))/60000*.8,1152-this.eventTop(b)));}
  eventWidth(){return 96/(this.courtFilter?1:Math.max(1,this.courts.length));}
  eventLeft(b:Booking){return this.courtFilter?2:2+this.courts.findIndex(c=>c.id===b.courtId)*this.eventWidth();}
  async ngOnInit(){this.interval=setInterval(()=>{this.now=this.serverAnchor+(performance.now()-this.performanceAnchor);this.checkSounds();},1000);this.refreshInterval=setInterval(()=>{if(this.actor&&!document.hidden)this.refresh();},30000);if(this.api.token)await this.restore();}
  ngOnDestroy(){clearInterval(this.interval);clearInterval(this.refreshInterval);this.stopAlert();this.audioContext?.close();}
  async run(fn:()=>Promise<void>){if(this.busy)return;this.busy=true;this.error='';this.notice='';try{await fn();}catch(e:any){this.error=e.message;}finally{this.busy=false;}}
  async signIn(){await this.run(async()=>{const res=await this.api.request<{token:string;actor:Actor}>('/auth/login','POST',this.login);this.api.save(res.token);this.actor=res.actor;localStorage.setItem('sports.business',this.login.tenantId);this.login.password='';await this.restore();});}
  async restore(){try{this.actor=await this.api.request<Actor>('/me');await this.refresh();}catch(e:any){this.error=e.message;if(!this.api.token)this.actor=undefined;}}
  async logout(){await this.run(async()=>{await this.api.request('/auth/logout','POST');this.api.clear();this.actor=undefined;this.bookings=[];this.active=[];this.audio=false;this.stopAlert();});}
  async refresh(){if(this.refreshing||!this.actor)return;this.refreshing=true;this.loading=true;try{
    const t=performance.now();const op=await this.api.request<Operations>('/operations/today');
    this.serverAnchor=Date.parse(op.serverTime)+(performance.now()-t)/2;this.performanceAnchor=performance.now();this.now=this.serverAnchor;
    this.settings=op.settings;this.courts=op.courts;this.active=op.active;this.upcoming=op.reservations;
    const days=this.days();const from=this.localInstant(days[0]+'T00:00');const to=this.localInstant(this.shiftDate(days[days.length-1],1)+'T00:00');
    this.bookings=await this.api.request<Booking[]>(`/reservations?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    [this.clients,this.teachers,this.prices]=await Promise.all([this.api.request<Contact[]>('/clients'),this.api.request<Contact[]>('/teachers'),this.api.request<any[]>('/pricing-rules')]);
    this.loyalty=await this.api.request<any[]>('/loyalty');
    this.lastSync=new Date().toISOString();
    if(this.initialCalendarScroll&&this.tab==='calendar'){setTimeout(()=>{const el=document.querySelector('.calendar-scroll');if(el){el.scrollTop=16*48;this.initialCalendarScroll=false;}},0);}
  }catch(e:any){this.error=e.message;if(!this.api.token)this.actor=undefined;}finally{this.refreshing=false;this.loading=false;}}
  dateKey(d:Date){return new Intl.DateTimeFormat('en-CA',{timeZone:this.settings.timezone,year:'numeric',month:'2-digit',day:'2-digit'}).format(d);}
  shiftDate(date:string,n:number){const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);}
  days(){if(this.calendarMode==='day'||this.tab==='pending'||window.matchMedia('(max-width:760px)').matches)return [this.selectedDate];const d=new Date(this.selectedDate+'T12:00:00Z');const monday=this.shiftDate(this.selectedDate,-((d.getUTCDay()+6)%7));return Array.from({length:7},(_,i)=>this.shiftDate(monday,i));}
  async move(n:number){this.selectedDate=this.shiftDate(this.selectedDate,n*(this.calendarMode==='week'&&this.tab!=='pending'&&!window.matchMedia('(max-width:760px)').matches?7:1));await this.refresh();}
  async selectTab(tab:string){this.tab=tab;this.error='';if(tab==='settings'&&this.actor?.role==='ADMIN'){await this.run(async()=>{[this.prices,this.users,this.venues]=await Promise.all([this.api.request<any[]>('/pricing-rules'),this.api.request<any[]>('/users'),this.api.request<any[]>('/venues')]);});}await this.refresh();}
  visible(date:string){return this.bookings.filter(b=>this.dateKey(new Date(b.start))===date&&(!this.courtFilter||b.courtId===this.courtFilter)).sort((a,b)=>a.start.localeCompare(b.start));}
  pending(){return this.bookings.filter(b=>b.status==='PENDING'&&(!this.courtFilter||b.courtId===this.courtFilter));}
  courtName(id:string){return this.courts.find(c=>c.id===id)?.name||'Cancha';}
  color(id:string){return this.courts.find(c=>c.id===id)?.color||'#167d69';}
  time(value:string){return new Intl.DateTimeFormat('es-CO',{timeZone:this.settings.timezone,hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(value));}
  dayLabel(date:string){return new Intl.DateTimeFormat('es-CO',{timeZone:'UTC',weekday:'short',day:'numeric',month:'short'}).format(new Date(date+'T12:00:00Z'));}
  localInput(value:string){return this.dateKey(new Date(value))+'T'+this.time(value);}
  localInstant(value:string){
    // Convert wall clock in the configured business zone; do not use the device timezone.
    const target=Date.parse(value+'Z');let instant=target;
    for(let n=0;n<3;n++){const parts=new Intl.DateTimeFormat('en-CA',{timeZone:this.settings.timezone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).formatToParts(new Date(instant));const p:any={};parts.forEach(v=>p[v.type]=v.value);const observed=Date.parse(`${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}Z`);instant+=target-observed;}
    return new Date(instant).toISOString();
  }
  open(b?:Booking,date=this.selectedDate){this.editing=b;this.form=b?{...b,start:this.localInput(b.start),end:this.localInput(b.end),outsideHoursConfirmed:false}:{courtId:this.courtFilter||this.courts.find(c=>c.active)?.id||'',guestName:'',teamName:'',phone:'',kind:'NORMAL',status:'PENDING',start:date+'T19:00',end:date+'T20:00',notes:'',outsideHoursConfirmed:false};if(!b)this.defaultDuration();this.editor=true;this.error='';}
  calendarCourts(){return this.courts.filter(c=>!this.courtFilter||c.id===this.courtFilter);}
  openSlot(date:string,minute:number,court:Court){
    if(this.busy||!court.active)return;
    this.open(undefined,date);
    this.form.courtId=court.id;
    this.form.start=date+'T'+this.minuteTime(minute);
    this.defaultDuration();
  }
  defaultDuration(){if(this.editing)return;const c=this.courts.find(c=>c.id===this.form.courtId);if(c&&this.form.start){const end=new Date(Date.parse(this.form.start+'Z')+c.durationMinutes*60000);this.form.end=end.toISOString().slice(0,16);}}
  selectClient(){const c=this.clients.find(c=>c.id===this.form.clientId);if(c){this.form.guestName=c.name;this.form.phone=c.phone||'';}}
  formPrice(){
    if(this.form.kind==='BLOCK')return null;
    if(this.editing&&this.form.courtId===this.editing.courtId&&this.form.start===this.localInput(this.editing.start)&&this.form.end===this.localInput(this.editing.end))return this.editing.price??null;
    return scheduledPrice(this.prices,this.form.courtId,this.form.start);
  }
  formQuote(){if(this.editing&&!['PENDING','CONFIRMED','IN_PROGRESS'].includes(this.editing.status))return reservationQuote(this.editing,this.loyalty);return priceQuote(this.formPrice(),availableRewardPercent(this.loyalty,this.form.clientId,this.form.kind,this.editing?.rewardPercent||0));}
  bookingQuote(b:Booking){return reservationQuote(b,this.loyalty);}
  promotionIsPending(b?:Booking){return !!b&&['PENDING','CONFIRMED','IN_PROGRESS'].includes(b.status)&&availableRewardPercent(this.loyalty,b.clientId,b.kind,b.rewardPercent||0)>0;}
  async save(){await this.run(async()=>{const body={...this.form,start:this.localInstant(this.form.start),end:this.localInstant(this.form.end),clientId:this.form.clientId||null,teacherId:this.form.teacherId||null};await this.api.request(this.editing?`/reservations/${this.editing.id}`:'/reservations',this.editing?'PATCH':'POST',body);this.editor=false;this.notice='Reserva guardada';await this.refresh();});}
  canEdit(b:Booking){return ['PENDING','CONFIRMED'].includes(b.status);}
  async action(b:Booking,action:string){
    let reason:string|null=null;let exceptionConfirmed=false;
    if(['cancel','cancel-rain','mark-no-show'].includes(action)){if(!confirm(action==='mark-no-show'?'¿Marcar que no asistió y liberar el horario?':'¿Cancelar esta reserva y liberar el horario?'))return;reason=prompt('Motivo (opcional)','');if(reason===null)return;}
    if(action==='start'){const delta=(this.now-Date.parse(b.start))/60000;if(delta<0||delta>this.settings.toleranceMinutes){exceptionConfirmed=confirm('Inicio fuera de la hora o tolerancia programada. ¿Confirmas iniciar ahora?');if(!exceptionConfirmed)return;}}
    await this.run(async()=>{const updated=await this.api.request<Booking>(`/reservations/${b.id}/${action}`,'POST',{version:b.version,reason,exceptionConfirmed,deviceId:this.device});this.notice=updated.rewardPercent?`Partido finalizado · beneficio aplicado: ${updated.rewardPercent}%${updated.finalPrice!=null?' · Valor de referencia: '+updated.finalPrice+' COP':''}`:'Acción guardada';await this.refresh();});
  }
  current(c:Court){return this.active.find(b=>b.courtId===c.id);}
  next(c:Court):Booking|undefined{return this.upcoming.filter(b=>b.courtId===c.id&&['CONFIRMED','PENDING'].includes(b.status)&&b.kind!=='BLOCK').sort((a,b)=>a.start.localeCompare(b.start))[0];}
  timing(b:Booking){return matchTiming(b,this.now,this.settings);}
  timer(b:Booking){return clockLabel(this.timing(b).remaining);}
  late(b:Booking){return !b.actualStartedAt&&this.now>Date.parse(b.start)+this.settings.toleranceMinutes*60000;}
  async finishNext(b:Booking,next?:Booking){if(!next||!confirm(`¿Finalizar ${b.guestName} e iniciar ${next.guestName}? Confirma que la cancha está entregada y las personas de la siguiente reserva listas.`))return;
    await this.run(async()=>{await this.api.request(`/reservations/${b.id}/finish`,'POST',{version:b.version});try{await this.api.request(`/reservations/${next.id}/start`,'POST',{version:next.version,deviceId:this.device,exceptionConfirmed:true});this.notice='Partido finalizado y siguiente iniciado';}catch(e:any){this.error='El partido anterior finalizó. El siguiente NO se confirmó como iniciado: '+e.message;}await this.refresh();});
  }
  async toggleSound(){if(this.audio){this.audio=false;this.stopAlert();return;}try{this.audioContext??=new AudioContext();await this.audioContext.resume();this.audio=true;this.beep();}catch{this.error='El navegador no permitió activar el sonido';}}
  private stopAlert(){for(const node of this.alertNodes){node.onended=null;try{node.stop();}catch{}node.disconnect();}this.alertNodes=[];this.alertGain?.disconnect();this.alertGain=undefined;}
  private beep(){
    const context=this.audioContext;if(!context||this.alertNodes.length)return;
    // Twelve alternating tones over eight seconds; avoid stacking simultaneous alerts.
    const gain=context.createGain();this.alertGain=gain;gain.connect(context.destination);
    const start=context.currentTime;gain.gain.setValueAtTime(0,start);
    for(let i=0;i<12;i++){
      const at=start+i*.7;
      gain.gain.setValueAtTime(0,at);
      gain.gain.linearRampToValueAtTime(.28,at+.03);
      gain.gain.setValueAtTime(.28,at+.42);
      gain.gain.linearRampToValueAtTime(0,at+.55);
    }
    const tone=context.createOscillator();tone.type='triangle';tone.connect(gain);
    for(let i=0;i<12;i++)tone.frequency.setValueAtTime(i%2?880:660,start+i*.7);
    this.alertNodes=[tone];
    tone.onended=()=>{tone.disconnect();gain.disconnect();this.alertGain=undefined;this.alertNodes=this.alertNodes.filter(n=>n!==tone);};
    tone.start(start);tone.stop(start+8.3);
  }
  private checkSounds(){if(!this.audio)return;for(const b of this.active){if(b.soundDevice!==this.device)continue;for(const key of alertKeys(b,this.now,this.settings)){if(!this.sounded.has(key)){this.beep();this.sounded.add(key);sessionStorage.setItem('sports.alerts',JSON.stringify([...this.sounded].slice(-500)));}}}}
  whatsapp(b:Booking){const phone=(b.phone||'').replace(/\D/g,'');if(!phone)return;const normalized=phone.length===10?'57'+phone:phone;const message=prompt('Revisa el mensaje antes de abrir WhatsApp',`Hola ${b.guestName}, ¿confirmas tu reserva en ${this.courtName(b.courtId)} el ${this.dayLabel(this.dateKey(new Date(b.start)))} a las ${this.time(b.start)}?`);if(message!==null)window.open(`https://wa.me/${normalized}?text=${encodeURIComponent(message)}`,'_blank','noopener,noreferrer');}
  loyaltyFor(id?:string){return this.loyalty.find(v=>v.clientId===id);}
  promotion(id?:string){const l=this.loyaltyFor(id);if(!l)return 'Sin promoción';if(!l.state.policy.active)return 'Promoción pausada';return l.state.rewardReady?`Beneficio disponible: ${l.state.policy.rewardPercent===100?'partido gratis':'50% de descuento'} en el próximo partido finalizado`:`${l.effectivePlayed} de ${l.state.policy.requiredMatches} partidos · ${l.state.policy.rewardPercent===100?'siguiente gratis':'siguiente al 50%'}${l.expired?' · racha vencida':''}`;}
  async openLoyalty(c:Contact){this.loyaltyClient=c;const l=this.loyaltyFor(c.id);this.loyaltyForm=l?{...l.state.policy}:{requiredMatches:5,rewardPercent:100,maxGapDays:21,active:true};this.loyaltyVersion=l?.state.version||0;this.exceptionDate='';this.exceptionReason='';await this.run(async()=>{this.loyaltyEvents=await this.api.request<any[]>(`/clients/${c.id}/loyalty/history`);});}
  async saveLoyalty(){if(!this.loyaltyClient)return;const id=this.loyaltyClient.id;await this.run(async()=>{await this.api.request(`/clients/${id}/loyalty`,'PUT',{policy:this.loyaltyForm,version:this.loyaltyVersion});await this.refresh();this.loyaltyVersion=this.loyaltyFor(id).state.version;this.loyaltyEvents=await this.api.request<any[]>(`/clients/${id}/loyalty/history`);this.notice='Promoción guardada';});}
  async extendLoyalty(){if(!this.loyaltyClient)return;const id=this.loyaltyClient.id;await this.run(async()=>{await this.api.request(`/clients/${id}/loyalty/exception`,'POST',{until:this.localInstant(this.exceptionDate+'T23:59'),reason:this.exceptionReason,version:this.loyaltyVersion});await this.refresh();this.loyaltyVersion=this.loyaltyFor(id).state.version;this.loyaltyEvents=await this.api.request<any[]>(`/clients/${id}/loyalty/history`);this.notice='Excepción registrada';});}
  loyaltyEvent(type:string){return ({POLICY_UPDATED:'Configuración actualizada',PLAYED:'Partido contabilizado',REDEEMED:'Beneficio aplicado',CYCLE_EXPIRED:'Racha reiniciada',GAP_EXCEPTION:'Prórroga justificada'} as any)[type]||type;}
  editClient(c:Contact){if(this.busy)return;this.contactTeacher=false;this.contact={id:c.id,name:c.name,phone:c.phone||'',notes:c.notes||''};this.error='';setTimeout(()=>{document.getElementById('contact-editor')?.scrollIntoView({behavior:'smooth',block:'center'});document.querySelector<HTMLInputElement>('#contact-editor input[name=phone]')?.focus({preventScroll:true});},0);}
  resetContact(){this.contact={name:'',phone:'',notes:''};this.contactTeacher=false;}
  async saveContact(){await this.run(async()=>{const editing=!!this.contact.id;const saved=await this.api.request<Contact>(this.contactTeacher?'/teachers':'/clients','POST',this.contact);if(this.loyaltyClient?.id===saved.id)this.loyaltyClient=saved;this.resetContact();await this.refresh();this.notice=editing?'Cliente actualizado':'Contacto guardado';});}

  async loadHistory(c:Contact){await this.run(async()=>{this.history=await this.api.request<Booking[]>(`/clients/${c.id}/history`);this.notice=`Historial de ${c.name} (hasta 200 registros)`;});}
  async saveVenue(){await this.run(async()=>{await this.api.request('/venues','POST',this.venueForm);this.venues=await this.api.request<any[]>('/venues');this.venueForm={name:'',address:''};this.notice='Sede guardada';});}
  async saveCourt(){await this.run(async()=>{await this.api.request('/courts','POST',this.courtForm);this.courtForm={name:'',sport:'Fútbol',capacity:10,durationMinutes:60,color:'#167d69',active:true};await this.refresh();this.notice='Cancha guardada';});}
  async saveSettings(){await this.run(async()=>{this.settings=await this.api.request<Settings>('/settings','PUT',this.settings);this.notice='Configuración guardada';});}
  pricingCourts(){return this.courts.filter(c=>!this.pricingCourtFilter||c.id===this.pricingCourtFilter);}
  dayPrices(court:string,day:number){return this.prices.filter(p=>p.courtId===court&&p.weekday===day).sort((a,b)=>a.fromMinute-b.fromMinute);}
  openPricing(p?:any,court?:string,day=1){this.pricing=p?{...p}:{courtId:court||this.pricingCourtFilter||this.courts[0]?.id||'',weekday:day,fromMinute:960,toMinute:1380,amount:0};this.pricingEditor=true;setTimeout(()=>{document.getElementById('pricing-editor')?.scrollIntoView({behavior:'smooth',block:'center'});document.querySelector<HTMLSelectElement>('#pricing-editor select')?.focus({preventScroll:true});},0);}
  async savePricing(){await this.run(async()=>{await this.api.request('/pricing-rules','POST',this.pricing);this.prices=await this.api.request<any[]>('/pricing-rules');this.pricingEditor=false;this.notice='Tarifa guardada. Las reservas existentes conservan su precio.';});}

  async generate(){await this.run(async()=>{const body={...this.recurrence,clientId:this.recurrence.clientId||null,id:this.recurrence.id||crypto.randomUUID()};this.recurrence.id=body.id;const created=await this.api.request<any[]>('/recurrence-rules','POST',body);await this.refresh();this.notice=`${created.length} reservas pendientes generadas`;});}
  async createUser(){await this.run(async()=>{await this.api.request('/users','POST',this.userForm);this.userForm={username:'',name:'',password:'',role:'EMPLOYEE'};this.users=await this.api.request<any[]>('/users');this.notice='Cuenta creada';});}
  async deactivate(user:any){if(!confirm(`¿Desactivar la cuenta de ${user.name}?`))return;await this.run(async()=>{await this.api.request(`/users/${user.id}/deactivate`,'POST');this.users=await this.api.request<any[]>('/users');});}
}
