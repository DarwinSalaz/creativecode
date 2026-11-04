import { test } from 'node:test';
import assert from 'node:assert/strict';
import { availableRewardPercent, priceQuote, scheduledPrice, reservationQuote } from '../src/app/reservations/price-quote.mjs';

const courtId = 'court-1';
const prices = [{ courtId, weekday: 1, fromMinute: 1080, toMinute: 1380, amount: 90000 }];

test('finds the court price by local weekday and start time', () => {
  assert.equal(scheduledPrice(prices, courtId, '2026-11-02T19:00'), 90000);
  assert.equal(scheduledPrice(prices, courtId, '2026-11-03T19:00'), null);
});

test('calculates the amount to collect with a half-price promotion', () => {
  assert.deepEqual(priceQuote(90000, 50), {
    basePrice: 90000,
    rewardPercent: 50,
    discount: 45000,
    finalPrice: 45000
  });
});

test('only previews an available reward for eligible linked bookings', () => {
  const loyalty = [{ clientId: 'client-1', state: { policy: { active: true, rewardPercent: 100 }, rewardReady: true } }];
  assert.equal(availableRewardPercent(loyalty, 'client-1', 'NORMAL'), 100);
  assert.equal(availableRewardPercent(loyalty, 'client-1', 'CLASS'), 0);
  assert.equal(availableRewardPercent(loyalty, undefined, 'NORMAL'), 0);
});

const ready = [{clientId:'client-1',state:{policy:{active:true,rewardPercent:100},rewardReady:true}}];
const finished = {clientId:'client-1',kind:'NORMAL',status:'FINISHED',price:90000,rewardPercent:0,finalPrice:90000};
test('fifth finished match remains full price when it earns the sixth free', () => {
  assert.equal(reservationQuote(finished,ready).finalPrice,90000);
  assert.equal(reservationQuote(finished,ready).rewardPercent,0);
  assert.equal(reservationQuote({...finished,status:'CONFIRMED'},ready).finalPrice,0);
});
test('historical reward stays applied after the current promotion changes', () => {
  assert.equal(reservationQuote({...finished,rewardPercent:50,finalPrice:45000},ready).finalPrice,45000);
  assert.equal(reservationQuote({...finished,rewardPercent:100,finalPrice:0},[]).finalPrice,0);
});
test('cancelled matches do not preview a future reward and missing prices stay unknown', () => {
  assert.equal(reservationQuote({...finished,status:'CANCELLED_CLIENT'},ready).finalPrice,90000);
  assert.equal(reservationQuote({...finished,price:null,finalPrice:null},ready),null);
});
