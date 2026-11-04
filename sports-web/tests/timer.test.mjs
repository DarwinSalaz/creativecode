import {test} from 'node:test';import assert from 'node:assert/strict';import {matchTiming,clockLabel,alertKeys} from '../src/app/operations/timer.mjs';
const b={id:'1',start:'2026-11-03T00:00:00Z',end:'2026-11-03T01:00:00Z',actualStartedAt:'2026-11-03T00:07:00Z'};
const s={waterMinutes:30,warningMinutes:5};
test('19:07 start gives water at 19:37, warning 20:02 and end 20:07',()=>{assert.equal(matchTiming(b,Date.parse('2026-11-03T00:36:59Z'),s).waterDue,false);assert.equal(matchTiming(b,Date.parse('2026-11-03T00:37:00Z'),s).waterDue,true);assert.equal(matchTiming(b,Date.parse('2026-11-03T01:02:00Z'),s).warning,true);assert.equal(matchTiming(b,Date.parse('2026-11-03T01:07:00Z'),s).finished,true);});
test('overrun remains visible and does not finish the reservation',()=>{assert.equal(clockLabel(matchTiming(b,Date.parse('2026-11-03T01:09:03Z'),s).remaining),'+02:03');assert.equal(b.actualEndedAt,undefined);});
test('delivered water suppresses repeated water alert',()=>{assert.equal(matchTiming({...b,waterDeliveredAt:b.actualStartedAt},Date.parse('2026-11-03T00:40:00Z'),s).waterDue,false);});
test('reload and offline reconstruction use same timestamps, not tick counts',()=>{assert.deepEqual(matchTiming({...b},Date.parse('2026-11-03T00:47:00Z'),s),matchTiming(b,Date.parse('2026-11-03T00:47:00Z'),s));assert.equal(matchTiming(b,Date.parse('2026-11-03T00:47:00Z'),s).remaining,1200);});
test('alert keys stable across refreshes',()=>{const now=Date.parse('2026-11-03T01:08:00Z');assert.deepEqual(alertKeys(b,now,s),alertKeys({...b},now,s));});
