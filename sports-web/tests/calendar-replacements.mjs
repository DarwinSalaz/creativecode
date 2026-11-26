import {chromium} from 'playwright-core';
import http from 'node:http';import fs from 'node:fs/promises';import path from 'node:path';import assert from 'node:assert/strict';
const root=path.resolve('dist/browser');
const server=http.createServer(async(req,res)=>{try{const file=path.resolve(root,decodeURIComponent(req.url.split('?')[0]).replace(/^\/sports\//,''));if(!file.startsWith(root+path.sep))throw Error();res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(await fs.readFile(file));}catch{res.writeHead(404);res.end();}});
await new Promise(r=>server.listen(0,'127.0.0.1',r));
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const errors=[];const writes=[];
try {
 const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});
 await context.addInitScript(()=>sessionStorage.setItem('sports.token','fixture'));
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 const courts=[1,2].map(i=>({id:'c'+i,name:'Cancha '+i,sport:'Fútbol',capacity:10,durationMinutes:60,color:'#167d69',active:true}));
 const bookings=['CANCELLED_CLIENT','CANCELLED_RAIN','NO_SHOW','CONFIRMED','PENDING'].map((status,i)=>({id:'b'+i,courtId:i===4?'c2':'c1',guestName:i===3?'Cliente vigente':'Cliente '+i,kind:'NORMAL',status,start:'2026-11-26T20:00:00-05:00',end:'2026-11-26T21:00:00-05:00',version:0}));
 await page.route('**/api/sports/v1/**',async route=>{const r=route.request();if(r.method()!=='GET')writes.push(r.url());const p=new URL(r.url()).pathname.replace('/api/sports/v1','');let body=[];
 if(p==='/me')body={id:'admin',tenantId:'fixture',name:'Admin',role:'ADMIN'};
 if(p==='/operations/today')body={serverTime:'2026-11-26T22:00:00Z',settings:{name:'Test',timezone:'America/Bogota',currency:'COP',toleranceMinutes:15,waterMinutes:30,warningMinutes:5},courts,active:[],reservations:[]};
 if(p==='/reservations')body=bookings;
 await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});});
 await page.goto(`http://127.0.0.1:${server.address().port}/sports/index.html`);
 await page.getByLabel('Fecha de agenda').fill('2026-11-26');await page.getByLabel('Fecha de agenda').dispatchEvent('change');
 await page.getByRole('button',{name:'Día',exact:true}).click();
 await page.waitForFunction(()=>document.querySelectorAll('.booking').length===2);
 assert.equal(await page.locator('.booking.cancelled').count(),0);
 assert.ok((await page.locator('.booking').allTextContents()).some(x=>x.includes('Cliente vigente')));
 await page.locator('.released-history summary').click();assert.equal(await page.locator('.released-row').count(),3);
 await page.getByLabel('Filtrar cancha').selectOption('c2');assert.equal(await page.locator('.booking').count(),1);assert.equal(await page.locator('.released-history').count(),0);
 await page.locator('.booking').first().click();
 const modal=page.getByRole('dialog');
 assert.equal(await modal.locator('form').count(),0);
 assert.equal(await modal.getByRole('button',{name:'Confirmar',exact:true}).isVisible(),true);
 await modal.getByRole('button',{name:'Cambiar reserva',exact:true}).click();
 assert.equal(await modal.locator('input[name=guest]').inputValue(),'Cliente 4');
 await modal.getByRole('button',{name:'Ocultar detalle',exact:true}).click();assert.equal(await modal.locator('form').count(),0);
 page.once('dialog',d=>d.dismiss());await modal.getByRole('button',{name:'Cancelar',exact:true}).click();
 assert.equal(await modal.isVisible(),true);assert.deepEqual(writes,[]);
 await modal.getByRole('button',{name:'Ver detalle de la reserva',exact:true}).click();assert.equal(await modal.locator('form').count(),1);
 await modal.getByRole('button',{name:'Cerrar',exact:true}).click();
 await page.getByLabel('Filtrar cancha').selectOption('c1');await page.locator('.released-history summary').click();
 await page.locator('.released-row').first().click();await page.getByRole('button',{name:'Reservar este horario para otra persona'}).click();
 assert.equal(await page.locator('input[name=guest]').inputValue(),'');assert.equal(await page.locator('input[name=start]').inputValue(),'2026-11-26T20:00');
 await page.keyboard.press('Escape');
 // Close the dialog using the actual UI, if Escape is not handled.
 const close=page.getByRole('button',{name:'Cerrar',exact:true});if(await close.count())await close.click();
 for(const width of [390,760]){await page.setViewportSize({width,height:844});assert.ok(await page.locator('.released-history').evaluate(e=>e.scrollWidth<=e.clientWidth));}
 assert.deepEqual(errors,[]);assert.deepEqual(writes,[]);
 console.log('PASS: replacements visible, cancelled/rain/no-show kept in history, court filter, open and replace cancelled booking, responsive history; no API writes.');
}finally{await browser.close();await new Promise(r=>server.close(r));}
