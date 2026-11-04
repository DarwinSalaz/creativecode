export function scheduledPrice(prices, courtId, localStart) {
  if (!courtId || !localStart || localStart.length < 16) return null;
  const date = new Date(`${localStart.slice(0, 10)}T12:00:00Z`);
  const weekday = date.getUTCDay() === 0 ? 7 : date.getUTCDay();
  const [hour, minute] = localStart.slice(11, 16).split(':').map(Number);
  const startMinute = hour * 60 + minute;
  const rule = prices.find(price => price.courtId === courtId && price.weekday === weekday &&
    startMinute >= price.fromMinute && startMinute < price.toMinute);
  return rule ? Number(rule.amount) : null;
}

export function availableRewardPercent(loyalty, clientId, kind, appliedPercent = 0) {
  if (appliedPercent > 0) return appliedPercent;
  if (!clientId || !['NORMAL', 'FIXED'].includes(kind)) return 0;
  const promotion = loyalty.find(value => value.clientId === clientId);
  return promotion?.state?.policy?.active && promotion.state.rewardReady
    ? Number(promotion.state.policy.rewardPercent || 0)
    : 0;
}

export function priceQuote(basePrice, rewardPercent) {
  if (basePrice === null || basePrice === undefined) return null;
  const base = Number(basePrice);
  const percent = Math.max(0, Math.min(100, Number(rewardPercent || 0)));
  return {
    basePrice: base,
    rewardPercent: percent,
    discount: base * percent / 100,
    finalPrice: base * (100 - percent) / 100
  };
}

// Closed bookings use their historical values, never the client's next benefit.
export function reservationQuote(booking, loyalty) {
  if (booking.kind === 'BLOCK') return null;
  const pending = ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(booking.status);
  if (pending) return priceQuote(booking.price, availableRewardPercent(loyalty, booking.clientId, booking.kind, booking.rewardPercent || 0));
  if (booking.price == null || booking.finalPrice == null) return null;
  return {basePrice: Number(booking.price), rewardPercent: booking.rewardPercent || 0,
    discount: Number(booking.price) - Number(booking.finalPrice), finalPrice: Number(booking.finalPrice)};
}
