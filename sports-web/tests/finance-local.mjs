import {chromium} from 'playwright-core';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const fixture=JSON.parse(await fs.readFile('../.local/sports-finance-test.json','utf8'));
const base='http://127.0.0.1:5002',api=base+'/api/sports/v1';
async function get(path,token=fixture.adminToken){const r=await fetch(api+path,{headers:{Authorization:'Bearer '+token}});assert.equal(r.status,200);return r.json();}
const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const errors=[];
async function session(token){const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});await context.addInitScript(token=>{sessionStorage.setItem('sports.token',token);sessionStorage.setItem('sports.api','/api/sports/v1');},token);const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto(base+'/sports/index.html');await page.getByRole('button',{name:'Caja y cierres'}).click();await page.locator('sports-finance').getByRole('heading',{name:'Cierre diario',exact:true}).waitFor();return {context,page,f:page.locator('sports-finance')};}
async function submitted(page,button,path){const response=page.waitForResponse(r=>r.url()===api+path&&r.request().method()==='POST');await button.click();const r=await response;assert.equal(r.status(),200,await r.text());await page.locator('sports-finance [role="status"]').waitFor();}
try{
 if(process.argv.includes('--preview')){
  const {page,f}=await session(fixture.adminToken);
  await f.getByRole('button',{name:'Resumen mensual',exact:true}).click();await f.getByRole('heading',{name:'Movimientos del mes'}).waitFor();
  await page.locator('ion-content').evaluate(el=>el.scrollToTop(0));assert.equal((await f.getByRole('button',{name:'Cierre diario',exact:true}).boundingBox()).y,(await f.getByRole('button',{name:'Cuentas y base',exact:true}).boundingBox()).y,'Desktop finance navigation shares one row');await page.waitForTimeout(200);await page.screenshot({path:'../docs/sports/screenshots/finance-month-desktop.png',fullPage:true});
  await page.setViewportSize({width:390,height:844});await f.getByRole('button',{name:'Cierre diario',exact:true}).click();await f.getByRole('button',{name:'Agregar transferencia'}).click();await f.getByRole('heading',{name:'Cierre diario',exact:true}).scrollIntoViewIfNeeded();await page.screenshot({path:'../docs/sports/screenshots/finance-daily-mobile.png',fullPage:true});
  for(const width of [320,390,760]){await page.setViewportSize({width,height:844});assert.equal(await f.evaluate(el=>el.scrollWidth<=el.clientWidth),true);}
  console.log('PASS: read-only finance previews, desktop navigation row and responsive widths.');
 }else{
 const employee=await session(fixture.employeeToken);const {page,f}=employee;
 const report=await get('/finance/report?month='+fixture.month);
 let testDate;
 for(let day=1;day<Number(fixture.today.slice(-2));day++){const d=fixture.month+'-'+String(day).padStart(2,'0');if(!report.days.some(x=>x.day===d)){testDate=d;break;}}
 if(testDate){
  await f.getByLabel('Fecha del cierre').fill(testDate);await f.getByLabel('Ingresos en efectivo (sin base)').fill('50000');await f.getByRole('button',{name:'Agregar transferencia'}).click();await f.getByLabel('Cuenta receptora').selectOption(fixture.bank);await f.getByLabel('Importe COP',{exact:true}).fill('70000');
  await submitted(page,f.getByRole('button',{name:'Guardar cierre diario'}),'/finance/days');
  const r=await get('/finance/report?month='+fixture.month);assert.ok(r.days.some(x=>x.day===testDate&&x.cash===50000&&x.transfers[0].amount===70000));
 }
 await f.getByRole('button',{name:'Gastos',exact:true}).click();await f.getByLabel('Dinero de donde se paga').selectOption(fixture.cash);await f.getByLabel('Importe del gasto COP').fill('10000');await f.getByLabel('Concepto',{exact:true}).fill('Compra de insumos desde navegador');
 await f.locator('input[type=file]').setInputFiles({name:'insumos.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.4\n1 0 obj << /Type /Catalog >> endobj\n%%EOF')});
 await submitted(page,f.getByRole('button',{name:'Guardar gasto'}),'/finance/expenses');
 await f.getByRole('button',{name:'Dinero y entregas',exact:true}).click();await f.getByLabel('Administrador que recibe').selectOption(fixture.admin);
 const before=await get('/finance',fixture.employeeToken);const expected=before.accounts.find(b=>b.account.id===fixture.cash).balance;
 await submitted(page,f.getByRole('button',{name:'Solicitar entrega completa'}),'/finance/handovers');
 const pending=(await get('/finance')).handovers.find(h=>h.status==='PENDING'&&h.senderId===fixture.employee);assert.equal(pending.expected,expected);
 await employee.context.close();
 const administrator=await session(fixture.adminToken);const p=administrator.page,ui=administrator.f;
 await ui.getByRole('button',{name:'Dinero y entregas',exact:true}).click();await ui.getByRole('button',{name:'Revisar y aceptar'}).click();await ui.getByLabel('Efectivo recibido',{exact:true}).fill(String(expected-5000));
 const note=ui.getByLabel('Nota del administrador');assert.equal(await note.getAttribute('required'),'');await note.fill('Faltante comprobado durante recepción de prueba');
 await submitted(p,ui.getByRole('button',{name:'Aceptar y registrar recepción'}),'/finance/handovers/'+pending.id+'/accept');
 await ui.getByRole('button',{name:'Cuentas y base',exact:true}).click();await ui.getByLabel('Nombre de la cuenta').fill('Nequi del administrador de prueba');await ui.getByLabel('Titular').selectOption(fixture.admin);await submitted(p,ui.getByRole('button',{name:'Agregar cuenta'}),'/finance/accounts');
 await ui.getByRole('button',{name:'Resumen mensual',exact:true}).click();await ui.getByRole('heading',{name:'Movimientos del mes'}).waitFor();await ui.getByText('Compra de insumos desde navegador',{exact:true}).waitFor();
 const download=p.waitForEvent('download');await ui.getByRole('button',{name:'Ver comprobante'}).click();assert.match((await download).suggestedFilename(),/\.pdf$/);
 const current=await get('/finance/report?month='+fixture.month);assert.equal(current.shortages,15000);assert.ok(current.entries.some(x=>x.kind==='EXPENSE'&&x.receiptId));
 await fs.mkdir('../docs/sports/screenshots',{recursive:true});await p.screenshot({path:'../docs/sports/screenshots/finance-month-desktop.png',fullPage:true});
 for(const width of [320,390,760]){
  await p.setViewportSize({width,height:844});
  for(const section of ['Cierre diario','Dinero y entregas','Gastos','Resumen mensual','Cuentas y base']){
   await ui.getByRole('button',{name:section,exact:true}).click();
   assert.equal(await ui.evaluate(el=>el.scrollWidth<=el.clientWidth),true,'Finance overflow at '+width+' '+section);
   for(const input of await ui.locator('input:not([type=checkbox]),select,textarea').all()){if(await input.isVisible()){const box=await input.boundingBox();assert.ok(box.x>=0&&box.x+box.width<=width+1,'Field fits mobile: '+section);}}
  }
 }
 await p.setViewportSize({width:390,height:844});await ui.getByRole('button',{name:'Cierre diario',exact:true}).click();await ui.getByRole('button',{name:'Agregar transferencia'}).click();const cashBox=await ui.getByLabel('Ingresos en efectivo (sin base)').boundingBox();const bankBox=await ui.getByLabel('Cuenta receptora').boundingBox();assert.ok(bankBox.y>cashBox.y);await p.screenshot({path:'../docs/sports/screenshots/finance-daily-mobile.png',fullPage:true});
 assert.deepEqual(errors,[]);console.log('PASS: real browser daily cash and transfer, expense with receipt download, complete delivery, admin acceptance with shortage, bank ownership, monthly report, 320/390/760 responsive layouts.');
}
}catch(error){const page=browser.contexts().at(-1)?.pages().at(-1);if(page)await page.screenshot({path:'/private/tmp/sports-finance-browser-failure.png',fullPage:true}).catch(()=>{});throw error;}finally{await browser.close();}
