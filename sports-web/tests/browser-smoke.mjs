import { chromium } from 'playwright-core';
import http from 'node:http';import fs from 'node:fs/promises';import path from 'node:path';import assert from 'node:assert/strict';
const root=path.resolve('dist/browser');
const server=http.createServer(async(req,res)=>{try{const relative=decodeURIComponent((req.url||'/').split('?')[0]).replace(/^\/sports\//,'');const file=path.resolve(root,relative||'index.html');if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}const body=await fs.readFile(file);const ext=path.extname(file);res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css','.svg':'image/svg+xml','.webmanifest':'application/manifest+json'})[ext]||'application/octet-stream');res.end(body);}catch{res.writeHead(404);res.end();}});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});
const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
const tenant='11111111-1111-4111-8111-111111111111';const actor={id:'22222222-2222-4222-8222-222222222222',tenantId:tenant,name:'Operador de prueba',role:'ADMIN'};
const settings={name:'Escenarios de prueba',timezone:'America/Bogota',currency:'COP',toleranceMinutes:15,waterMinutes:30,warningMinutes:5};
const courts=[{id:'33333333-3333-4333-8333-333333333333',name:'Cancha 1',sport:'Fútbol 5',capacity:10,durationMinutes:60,color:'#167d69',active:true},{id:'44444444-4444-4444-8444-444444444444',name:'Cancha 2',sport:'Fútbol 5',capacity:10,durationMinutes:60,color:'#bc8053',active:true}];
let rates=[{id:'rate-1',courtId:courts[0].id,weekday:1,fromMinute:960,toMinute:1080,amount:90000},{id:'rate-2',courtId:courts[0].id,weekday:1,fromMinute:1080,toMinute:1440,amount:120000}];let loyalty=[];let bookings=[];let writes=0;const requests=[];
await page.route('**/api/sports/v1/**',async route=>{
 const request=route.request();const url=new URL(request.url());const endpoint=url.pathname.replace('/api/sports/v1','');requests.push(endpoint);let body;
 if(endpoint==='/auth/login')body={token:'browser-fixture',actor,expiresAt:new Date(Date.now()+3600000).toISOString()};
 else if(endpoint==='/me')body=actor;
 else if(endpoint==='/operations/today')body={serverTime:new Date().toISOString(),settings,courts,active:bookings.filter(b=>b.status==='IN_PROGRESS'),reservations:bookings};
 else if(endpoint==='/reservations'&&request.method()==='POST'){const input=request.postDataJSON();assert.equal(input.guestName,'Carlos Pérez');assert.equal(input.status,'PENDING');assert.match(input.start,/Z$/);const b={...input,id:crypto.randomUUID(),version:0};bookings.push(b);writes++;body=b;}
 else if(endpoint==='/reservations')body=bookings;
 else if(endpoint==='/pricing-rules'&&request.method()==='POST'){const p=request.postDataJSON();if(p.id){rates=rates.map(r=>r.id===p.id?p:r);}else rates.push({...p,id:crypto.randomUUID()});body=p;}
 else if(endpoint==='/pricing-rules')body=rates;
 else if(endpoint==='/loyalty')body=loyalty;
 else if(['/clients','/teachers','/pricing-rules','/users','/venues'].includes(endpoint))body=[];
 else if(endpoint.endsWith('/finish')){const b=bookings.find(b=>endpoint.includes(b.id));assert.equal(request.postDataJSON().version,b.version);b.status='FINISHED';b.actualEndedAt=new Date().toISOString();b.version++;writes++;body=b;}
 else throw new Error('Unmocked endpoint: '+endpoint);
 await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
});
try {
 const base=`http://127.0.0.1:${server.address().port}/sports/`;
 await page.goto(base+'index.html');await page.getByRole('heading',{name:'Tu jornada empieza aquí'}).waitFor();
 await fs.mkdir('../docs/sports/screenshots',{recursive:true});
 await page.screenshot({path:'../docs/sports/screenshots/login-desktop.png',fullPage:true});
 await page.getByLabel('Negocio',{exact:true}).fill(tenant);await page.getByLabel('Usuario',{exact:true}).fill('test');await page.getByLabel('Contraseña',{exact:true}).fill('fixture-password');await page.getByRole('button',{name:'Entrar a mi negocio'}).click();
 await page.getByRole('heading',{name:'Una buena jornada empieza con orden.'}).waitFor();
 const day=await page.getByLabel('Fecha de agenda').inputValue();
 await page.getByRole('button',{name:`Reservar Cancha 2 el ${day} a las 18:00`,exact:true}).click();
 assert.equal(await page.getByLabel('Inicio programado').inputValue(),day+'T18:00');
 assert.equal(await page.getByLabel('Fin programado').inputValue(),day+'T19:00');
 assert.equal(await page.locator('.modal select[name="court"]').inputValue(),courts[1].id);
 await page.getByRole('button',{name:'Cerrar',exact:true}).click();
 await page.setViewportSize({width:390,height:844});
 await page.getByLabel('Filtrar cancha').selectOption(courts[0].id);
 await page.getByRole('button',{name:`Reservar Cancha 1 el ${day} a las 23:00`,exact:true}).click();
 assert.equal(await page.getByLabel('Inicio programado').inputValue(),day+'T23:00');
 const tomorrow=new Date(day+'T12:00:00Z');tomorrow.setUTCDate(tomorrow.getUTCDate()+1);
 assert.equal(await page.getByLabel('Fin programado').inputValue(),tomorrow.toISOString().slice(0,10)+'T00:00');
 await page.getByRole('button',{name:'Cerrar',exact:true}).click();
 await page.getByLabel('Filtrar cancha').selectOption('');
 await page.setViewportSize({width:1440,height:1000});
 await page.getByRole('button',{name:'Nueva reserva'}).click();await page.getByLabel('Nombre del cliente').fill('Carlos Pérez');
 const start=page.getByLabel('Inicio programado'),end=page.getByLabel('Fin programado');
 assert.equal((await start.boundingBox()).y,(await end.boundingBox()).y,'Desktop dates share a row');
 for(const width of [320,390,760]){
  await page.setViewportSize({width,height:844});
  const fields=await page.locator('.form-grid input,.form-grid select').all();
  for(const field of fields){const box=await field.boundingBox();assert.ok(box.x>=0&&box.x+box.width<=width,'Field fits mobile viewport');}
  assert.ok((await end.boundingBox()).y>(await start.boundingBox()).y,'Mobile dates stack');
  assert.equal(await page.locator('.modal').evaluate(el=>el.scrollWidth<=el.clientWidth),true,'No modal horizontal overflow');
 }
 await page.screenshot({path:'../docs/sports/screenshots/reservation-mobile.png',fullPage:true});
 await page.setViewportSize({width:1440,height:1000});
await page.getByRole('button',{name:'Guardar reserva',exact:true}).click();await page.getByText('Reserva guardada',{exact:true}).waitFor();assert.equal(writes,1);
 await page.locator('.calendar-scroll').evaluate(el=>el.scrollTop=16*48);await page.screenshot({path:'../docs/sports/screenshots/calendar-desktop.png',fullPage:true});
 bookings[0]={...bookings[0],status:'FINISHED',clientId:'client-price-test',price:90000,rewardPercent:0,finalPrice:90000};
 loyalty=[{clientId:'client-price-test',state:{policy:{active:true,rewardPercent:100},rewardReady:true}}];
 await page.getByRole('button',{name:/Actualizar/}).click();
 await page.getByRole('button',{name:/Carlos Pérez/}).first().click();
 const total=page.locator('.modal .price-total strong');
 assert.equal((await total.innerText()).replace(/[^0-9]/g,''),'90000','Finished fifth match must retain full price despite next reward');
 assert.equal(await page.locator('.modal').getByText(/Promoción disponible/).count(),0);
 await page.getByRole('button',{name:'Cerrar',exact:true}).click();
 loyalty=[];
 bookings[0]={...bookings[0],status:'IN_PROGRESS',actualStartedAt:new Date(Date.now()-31*60000).toISOString(),version:1};
 await page.getByRole('button',{name:'En cancha'}).click();await page.getByText('Agua pendiente',{exact:false}).waitFor();await page.screenshot({path:'../docs/sports/screenshots/operations-desktop.png',fullPage:true});
 await page.evaluate(()=>{
  window.audioCheck={starts:[],stops:[]};
  const create=AudioContext.prototype.createOscillator;
  AudioContext.prototype.createOscillator=function(){const node=create.call(this);const start=node.start.bind(node),stop=node.stop.bind(node);node.start=(t)=>{window.audioCheck.starts.push(t);start(t);};node.stop=(t)=>{window.audioCheck.stops.push(t??null);stop(t);};return node;};
 });
 await page.getByRole('button',{name:'♩ Activar sonido'}).click();
 const audio=await page.evaluate(()=>window.audioCheck);
 assert.equal(audio.starts.length,1);
 assert.ok(audio.stops[0]-audio.starts[0]>=8,'Alert lasts at least eight seconds');
 await page.getByRole('button',{name:'♪ Sonido activo · silenciar'}).click();
 assert.equal(await page.evaluate(()=>window.audioCheck.stops.at(-1)),null,'Mute stops current alert immediately');
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:'../docs/sports/screenshots/operations-mobile.png',fullPage:true});
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true,'Mobile horizontal overflow');
 const initial=await page.locator('.timer').innerText();await page.route('**/api/sports/v1/operations/today',route=>route.abort());await page.getByRole('button',{name:'Actualizar',exact:false}).click();await page.getByText('Sin conexión · datos anteriores').waitFor();await page.waitForTimeout(2200);assert.notEqual(await page.locator('.timer').innerText(),initial,'Offline timer must keep moving');
 await page.unroute('**/api/sports/v1/operations/today');
 await page.setViewportSize({width:1440,height:1000});
 await page.getByRole('button',{name:/Administración/}).click();
 const panel=page.locator('.pricing-panel');
 await panel.getByRole('heading',{name:'Tarifas de la semana'}).waitFor();
 assert.equal(await panel.locator('.pricing-day').count(),14,'All seven days for each court');
 await panel.getByRole('button',{name:'Editar tarifa Cancha 1 Lunes 18:00',exact:true}).click();
 assert.equal(await page.getByLabel('Hora de fin',{exact:false}).inputValue(),'00:00');
 await page.getByLabel('Precio por reserva (COP)').fill('125000');
 await page.getByRole('button',{name:'Guardar cambios',exact:true}).click();
 await page.locator('#pricing-editor').waitFor({state:'hidden'});
 assert.equal(rates.find(r=>r.id==='rate-2').toMinute,1440);
 assert.equal(rates.find(r=>r.id==='rate-2').amount,125000);
 await panel.getByRole('button',{name:'Agregar tarifa Cancha 1 Domingo',exact:true}).click();
 assert.equal(await page.locator('#pricing-editor select[name=day] option:checked').innerText(),'Domingo');
 await page.getByLabel('Precio por reserva (COP)').fill('80000');
 await page.getByRole('button',{name:'Guardar tarifa',exact:true}).click();
 await page.locator('#pricing-editor').waitFor({state:'hidden'});
 assert.equal(rates.length,3,'Adding after editing creates a new rate');
 await page.getByLabel('Mostrar tarifas de').selectOption(courts[0].id);
 assert.equal(await panel.locator('.pricing-day').count(),7);
 await panel.screenshot({path:'../docs/sports/screenshots/pricing-desktop.png'});
 for(const width of [320,390]){
  await page.setViewportSize({width,height:844});
  assert.ok(await panel.evaluate(el=>el.scrollWidth<=el.clientWidth),'Rates do not overflow on mobile');
 }
 await panel.screenshot({path:'../docs/sports/screenshots/pricing-mobile.png'});
 assert.equal(errors.length,0,errors.join('\n'));
 console.log('PASS: desktop login, API-backed reservation form with mocked transport, calendar, operations, mobile layout, offline clock.');console.log(`Intercepted ${requests.length} API requests; ${writes} in-memory fixture writes; zero database connections.`);
} finally {await browser.close();await new Promise(resolve=>server.close(resolve));}
