// Explicit local integration test. No route mocks, no remote endpoints.
import {chromium} from 'playwright-core';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const config=JSON.parse(await fs.readFile('../.local/sports-local.json','utf8'));
const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1000},timezoneId:'America/Bogota'});
 const errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.route('**/*',route=>{const u=new URL(route.request().url());if(u.hostname==='127.0.0.1'&&u.port==='5002')return route.continue();return route.abort();});
 await page.goto('http://127.0.0.1:5002/sports/index.html');
 await page.getByLabel('Identificador del negocio').fill('11111111-1111-4111-8111-111111111111');
 await page.getByLabel('Usuario',{exact:true}).fill('admin');
 await page.getByLabel('Contraseña',{exact:true}).fill(config.loginPassword);
 await page.getByRole('button',{name:'Entrar a mi negocio'}).click();
 await page.getByRole('heading',{name:'Una buena jornada empieza con orden.'}).waitFor();
 await page.getByRole('button',{name:'Nueva reserva'}).click();
 await page.getByLabel('Nombre del cliente').fill('Laura Gómez');
 await page.getByLabel('Nombre del equipo (opcional)',{exact:true}).fill('Los amigos');
 const date=new Date();date.setUTCDate(date.getUTCDate()+10+Math.floor(Math.random()*300));
 const day=date.toISOString().slice(0,10);
 const minute=Math.floor(Date.now()/60000)%60;
 await page.getByLabel('Inicio programado').fill(`${day}T18:${String(minute).padStart(2,'0')}`);
 await page.getByLabel('Autorizo reservar fuera').check();
 await page.getByRole('button',{name:'Guardar reserva',exact:true}).click();
 await page.getByText('Reserva guardada',{exact:true}).waitFor();
 await page.getByLabel('Fecha de agenda').fill(day);await page.getByLabel('Fecha de agenda').dispatchEvent('change');
 await page.getByRole('button',{name:/Laura Gómez/}).first().waitFor();
 const card=page.getByRole('button',{name:/Laura Gómez/}).first();
 assert.equal((await card.innerText()).includes('Los amigos'),false);
 await card.click();
 assert.equal(await page.getByLabel('Nombre del cliente').inputValue(),'Laura Gómez');
 assert.equal(await page.getByLabel('Nombre del equipo (opcional)',{exact:true}).inputValue(),'Los amigos');
 await page.getByRole('button',{name:'Cerrar',exact:true}).click();
 await page.screenshot({path:'../docs/sports/screenshots/calendar-local-real.png',fullPage:true});
 assert.equal(errors.length,0,errors.join('\n'));
 console.log('PASS: real local browser login + reservation persisted via Kotlin API + calendar. Remote requests blocked.');
} finally {await browser.close();}
