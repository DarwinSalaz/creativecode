import {chromium} from 'playwright-core';
import fs from 'node:fs/promises';import assert from 'node:assert/strict';
const fixture=JSON.parse(await fs.readFile('../.local/sports-investment-test.json','utf8'));
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const errors=[];
async function open(token){const context=await browser.newContext({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});await context.addInitScript(t=>sessionStorage.setItem('sports.token',t),token);const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:5002/sports/index.html');return {page,context};}
try{
 const {page,context}=await open(fixture.adminToken);
 await page.getByRole('button',{name:'Inversiones',exact:false}).click();const f=page.locator('sports-investments');
 await f.locator('.investment-record').first().waitFor();
 await page.waitForFunction(()=>document.querySelectorAll('.investment-record').length===1 && !document.querySelector('sports-investments .investment-heading>button').disabled);
 assert.equal(await f.locator('.investment-record').count(),1);
 await f.getByRole('button',{name:'Registrar inversión'}).click();
 await f.getByLabel('Total de la inversión (COP)',{exact:true}).fill('120000');
 await f.getByLabel('Concepto',{exact:true}).fill('Arreglo local de redes para prueba de inversiones');
 await f.locator('select[name=category]').selectOption('REPAIR');
 await f.getByRole('button',{name:'Agregar otro administrador'}).click();
 const amounts=f.getByLabel('Aporte (COP)',{exact:true});
 await amounts.nth(0).fill('50000');await amounts.nth(1).fill('50000');
 assert.equal(await f.getByRole('button',{name:'Guardar inversión',exact:true}).isDisabled(),true);
 await amounts.nth(1).fill('70000');
 assert.equal(await f.getByRole('button',{name:'Guardar inversión',exact:true}).isEnabled(),true);
 for(const width of [320,390,760]){await page.setViewportSize({width,height:844});assert.ok(await f.evaluate(e=>e.scrollWidth<=e.clientWidth),'Form fits screen at '+width);assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page fits screen');}
 await f.getByRole('button',{name:'Guardar inversión',exact:true}).click();
 await f.getByText('Inversión registrada. Los saldos de caja no cambian.',{exact:true}).waitFor();
 await f.getByRole('button',{name:'Registrar inversión'}).waitFor();
 await page.waitForFunction(()=>document.querySelectorAll('.investment-record').length===2);
 await f.locator('.investment-record summary').first().click();await f.getByRole('heading',{name:'Aportes de esta inversión'}).waitFor();
 assert.ok((await f.locator('.investment-total').innerText()).includes('1,120,000'));
 for(const width of [320,390,760]){await page.setViewportSize({width,height:844});assert.ok(await f.evaluate(e=>e.scrollWidth<=e.clientWidth),'History fits screen');}
 await page.setViewportSize({width:390,height:844});await f.screenshot({path:'../.local/investments-mobile.png'});
 await page.setViewportSize({width:1440,height:1000});await f.screenshot({path:'../.local/investments-desktop.png'});
 await f.getByRole('button',{name:'Todos los meses'}).click();await page.waitForFunction(()=>document.querySelectorAll('.investment-record').length===3);
 await context.close();
 const employee=await open(fixture.employeeToken);await employee.page.getByRole('button',{name:'Caja y cierres'}).waitFor();
 assert.equal(await employee.page.getByRole('button',{name:'Inversiones',exact:false}).count(),0);
 const status=await employee.page.evaluate(async t=>(await fetch('/api/sports/v1/investments',{headers:{Authorization:'Bearer '+t}})).status,fixture.employeeToken);assert.equal(status,403);
 assert.deepEqual(errors,[]);
 console.log('PASS: local browser; shared investment registration, allocation validation, cumulative totals, month/history detail, admin-only menu/API and 320/390/760 responsive layouts.');
}finally{await browser.close();}
