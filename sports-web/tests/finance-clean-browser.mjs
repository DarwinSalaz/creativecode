import {chromium} from 'playwright-core';
import http from 'node:http';import fs from 'node:fs/promises';import path from 'node:path';import assert from 'node:assert/strict';
const root=path.resolve('dist/browser');
const server=http.createServer(async(req,res)=>{try{const file=path.resolve(root,decodeURIComponent(req.url.split('?')[0]).replace(/^\/sports\//,''));if(!file.startsWith(root+path.sep))throw Error();res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(await fs.readFile(file));}catch{res.writeHead(404);res.end();}});
await new Promise(r=>server.listen(0,'127.0.0.1',r));
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const errors=[],writes=[];
const settings={name:'Prueba visual',timezone:'America/Bogota',currency:'COP',toleranceMinutes:15,waterMinutes:30,warningMinutes:5};
const people=[{id:'admin',name:'Laura',role:'ADMIN',active:true},{id:'worker',name:'Carlos',role:'EMPLOYEE',active:true}];
const handovers=[{id:'pending',senderId:'worker',receiverId:'admin',expected:200000,status:'PENDING',requestedDay:'2026-11-10'},{id:'accepted',senderId:'worker',receiverId:'admin',expected:300000,received:295000,status:'ACCEPTED',requestedDay:'2026-10-28',acceptedDay:'2026-10-30',note:'Se recibió con faltante documentado.'}];
async function open(role){
 const actor={...people.find(p=>p.role===role),tenantId:'fixture'};
 const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});
 await context.addInitScript(()=>sessionStorage.setItem('sports.token','fixture'));
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 const accounts=[{account:{id:'cash',name:'Efectivo de '+actor.name,kind:'CASH',ownerId:actor.id},balance:475000},{account:{id:'bank',name:'Cuenta de ahorros',kind:'BANK',ownerId:actor.id},balance:500000}];
 const movements=[{entry:{id:'one',day:'2026-11-10',kind:'INCOME',amount:200000,actorId:'admin',note:'Cierre diario registrado'},balanceAfter:475000,sourceDays:[{id:'day',day:'2026-11-10',actorId:'admin',cash:200000}]},{entry:{id:'two',day:'2026-11-09',kind:'EXPENSE',amount:-20000,actorId:'admin',note:'Compra de insumos para la cancha y artículos de limpieza para el mantenimiento semanal.'},balanceAfter:275000,sourceDays:[]},{entry:{id:'three',day:'2026-10-30',kind:'HANDOVER_IN',amount:295000,actorId:'admin'},balanceAfter:295000,sourceDays:[{id:'old-day',day:'2026-10-28',actorId:'worker',cash:300000}],handover:handovers[1]}];
 await page.route('**/api/sports/v1/**',async route=>{const r=route.request();if(r.method()!=='GET')writes.push(r.url());const u=new URL(r.url()),p=u.pathname.replace('/api/sports/v1','');let body=[];
  if(p==='/me')body=actor;
  else if(p==='/operations/today')body={serverTime:'2026-11-11T02:00:00Z',settings,courts:[],active:[],reservations:[]};
  else if(p==='/finance')body={today:'2026-11-10',cashBase:150000,people,accounts,pendingDays:[{day:'2026-11-10',cash:200000}],handovers};
  else if(p==='/finance/report')body={month:'2026-11',closed:false,days:[],entries:[],balances:[],handovers:[]};
  else if(p.includes('/statement'))body={account:accounts.find(a=>p.includes(a.account.id)).account,month:'2026-11',opening:0,incoming:495000,outgoing:20000,closing:475000,movements};
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto(`http://127.0.0.1:${server.address().port}/sports/index.html`);await page.getByRole('button',{name:'Caja y cierres'}).click();
 return {page,context,f:page.locator('sports-finance')};
}
try{
 const {page,context,f}=await open('ADMIN');
 await f.locator('.movement-summary').first().waitFor();
 assert.equal(await f.locator('.movement[open]').count(),0);
 assert.equal(await f.getByRole('heading',{name:'Entregas y traslados por aceptar'}).count(),0);
 assert.equal(await f.locator('.closing-origins').first().isVisible(),false);
 await f.locator('.movement-summary').first().focus();await page.keyboard.press('Enter');
 assert.equal(await f.locator('.closing-origins').first().isVisible(),true);
 await f.locator('.movement-summary').first().click();
 await fs.mkdir('../.local',{recursive:true});await f.locator('.account-statement').screenshot({path:'../.local/finance-clean-desktop.png'});
 for(const width of [320,390,760]){await page.setViewportSize({width,height:844});assert.ok(await f.evaluate(e=>e.scrollWidth<=e.clientWidth));const row=f.locator('.movement-summary').first();assert.ok((await row.boundingBox()).height<110,'Compact movement row');await row.click();assert.ok(await f.evaluate(e=>e.scrollWidth<=e.clientWidth));await row.click();}
 await page.setViewportSize({width:390,height:844});await f.locator('.account-statement').screenshot({path:'../.local/finance-clean-mobile.png'});
 await f.getByRole('button',{name:/^Entregas/}).click();await f.getByRole('heading',{name:'Entregas e historial',exact:true}).waitFor();
 assert.equal(await f.locator('.movement-summary').count(),0);
 assert.equal(await f.locator('.handover-item').count(),1);
 await f.getByLabel('Mes de recepción').fill('2026-11');assert.equal(await f.locator('.handover-item').count(),0);
 await f.getByRole('button',{name:'Ver todo el historial'}).click();
 await f.locator('.handover-summary').click();await f.getByText('Se recibió con faltante documentado.',{exact:true}).waitFor();
 await f.getByRole('button',{name:'Revisar y aceptar'}).click();await f.getByLabel('Efectivo recibido',{exact:true}).waitFor();
 await f.getByRole('button',{name:'Cancelar',exact:true}).click();
 await f.getByRole('button',{name:'Trasladar efectivo (opcional)',exact:true}).click();await f.getByRole('heading',{name:'Trasladar mi efectivo'}).waitFor();
 for(const width of [320,390,760]){await page.setViewportSize({width,height:844});assert.ok(await f.evaluate(e=>e.scrollWidth<=e.clientWidth),'Delivery mobile layout');}
 await page.setViewportSize({width:390,height:844});await f.screenshot({path:'../.local/finance-deliveries-mobile.png'});
 await context.close();
 const worker=await open('EMPLOYEE');await worker.f.getByRole('button',{name:'Preparar entrega →'}).click();await worker.f.getByRole('heading',{name:'Entregar todo mi efectivo'}).waitFor();
 assert.deepEqual(writes,[]);assert.deepEqual(errors,[]);
 console.log('PASS: compact expandable movements, keyboard detail, separate pending deliveries and history, month filter, admin optional transfer, employee delivery, 320/390/760 layouts. Mocked API; no database writes.');
}finally{await browser.close();await new Promise(r=>server.close(r));}
