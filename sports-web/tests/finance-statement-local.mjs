import {chromium} from 'playwright-core';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const fixture=JSON.parse(await fs.readFile('../.local/sports-finance-test.json','utf8'));
const base='http://127.0.0.1:5002';
const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const errors=[];
async function open(token){const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});await context.addInitScript(token=>{sessionStorage.setItem('sports.token',token);sessionStorage.setItem('sports.api','/api/sports/v1');},token);const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(base+'/sports/index.html');await page.getByRole('button',{name:'Caja y cierres'}).click();return {context,page,f:page.locator('sports-finance')};}
try {
 const {context,page,f}=await open(fixture.adminToken);
 await f.getByRole('heading',{name:'Mi dinero y movimientos'}).waitFor();
 await f.locator('.movement').first().waitFor();
 assert.equal(await f.getByRole('button',{name:'Solicitar entrega completa'}).count(),0);
 assert.equal(await f.getByRole('heading',{name:'Tu efectivo pendiente'}).count(),0);
 assert.equal(await f.getByRole('button',{name:'Preparar entrega →'}).count(),0);
 const reportResponse=page.waitForResponse(r=>r.url().includes('/accounts/'+fixture.adminCash+'/statement?month='+fixture.previousMonth));
 await f.getByLabel('Mes de movimientos').fill(fixture.previousMonth);await f.getByLabel('Mes de movimientos').dispatchEvent('change');await reportResponse;
 await f.getByText('No hay movimientos en este mes. El saldo inicial se conserva.').waitFor();
 const response=page.waitForResponse(r=>r.url().includes('/accounts/'+fixture.adminCash+'/statement?month='+fixture.month));
 await f.getByLabel('Mes de movimientos').fill(fixture.month);await f.getByLabel('Mes de movimientos').dispatchEvent('change');const data=await (await response).json();
 assert.ok(data.movements.some(m=>m.sourceDays.some(d=>d.day.startsWith(fixture.previousMonth))));
 assert.equal(await f.locator('.movement[open]').count(),0);
 for(const summary of await f.locator('.movement-summary').all())await summary.click();
 await f.locator('.closing-origins').first().waitFor();
 const dateLabel=day=>new Intl.DateTimeFormat('es-CO',{day:'numeric',month:'short',year:'numeric',timeZone:'UTC'}).format(new Date(day+'T12:00:00Z'));
 const oldSource=data.movements.flatMap(m=>m.sourceDays).find(d=>d.day.startsWith(fixture.previousMonth));
 assert.ok((await f.locator('.closing-origins').allTextContents()).some(s=>s.includes(dateLabel(oldSource.day))));
 await f.getByRole('button',{name:/^Entregas/}).click();
 await f.getByRole('heading',{name:'Historial de entregas',exact:true}).waitFor();
 await f.getByRole('button',{name:'Trasladar efectivo (opcional)',exact:true}).click();await f.getByRole('heading',{name:'Trasladar mi efectivo',exact:true}).waitFor();
 assert.equal(await f.getByRole('button',{name:'Solicitar traslado completo'}).isEnabled(),true);
 await f.getByRole('button',{name:'Ocultar traslado',exact:true}).click();
 await f.getByRole('button',{name:'Mi dinero',exact:true}).click();
 await page.locator('ion-content').evaluate(el=>el.scrollToTop(0));await page.screenshot({path:'../docs/sports/screenshots/finance-statement-desktop.png',fullPage:true});
 await f.getByRole('button',{name:'Cierre diario',exact:true}).click();await f.getByRole('heading',{name:'Efectivo a tu cargo',exact:true}).waitFor();
 assert.equal(await f.getByRole('button',{name:'Preparar entrega →'}).count(),0);
 await f.getByRole('button',{name:'Ver mi saldo y movimientos →'}).click();await f.locator('.movement').first().waitFor();
 for(const width of [320,390,760]){
  await page.setViewportSize({width,height:844});
  assert.equal(await f.evaluate(el=>el.scrollWidth<=el.clientWidth),true,'No overflow at '+width);
  for(const field of await f.locator('input,select,textarea').all()){if(await field.isVisible()){const box=await field.boundingBox();assert.ok(box.x>=0&&box.x+box.width<=width+1);}}
 }
 await page.setViewportSize({width:390,height:844});await f.getByRole('heading',{name:'Detalle de movimientos'}).scrollIntoViewIfNeeded();await page.screenshot({path:'../docs/sports/screenshots/finance-statement-mobile.png',fullPage:true});
 await context.close();
 const employee=await open(fixture.employeeToken);await employee.f.getByRole('heading',{name:'Tu efectivo pendiente',exact:true}).waitFor();
 assert.equal(await employee.f.getByRole('button',{name:'Preparar entrega →'}).count(),1);
 assert.deepEqual(errors,[]);
 console.log('PASS: admin custody default, optional transfer, historical closing dates, month switching, employee delivery preserved, responsive statement at 320/390/760px; no financial writes.');
}finally{await browser.close();}
