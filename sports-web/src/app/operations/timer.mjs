/** Pure timestamp arithmetic; timers continue without any network requests. */
export function matchTiming(booking, now, settings) {
  const start=Date.parse(booking.actualStartedAt);
  const duration=(Date.parse(booking.end)-Date.parse(booking.start))/1000;
  const elapsed=Math.max(0,Math.floor((now-start)/1000));
  const remaining=Math.ceil(duration-elapsed);
  return {elapsed,remaining,waterDue:elapsed>=settings.waterMinutes*60&&!booking.waterDeliveredAt,warning:remaining<=settings.warningMinutes*60&&remaining>0,finished:remaining<=0};
}
export function clockLabel(seconds){const n=Math.abs(seconds);return `${seconds<0?'+':''}${Math.floor(n/60).toString().padStart(2,'0')}:${Math.floor(n%60).toString().padStart(2,'0')}`;}
export function alertKeys(booking, now, settings){const t=matchTiming(booking,now,settings);return [t.waterDue?'water':null,t.warning?'warning':null,t.finished?'end':null].filter(Boolean).map(kind=>`${booking.id}:${booking.actualStartedAt}:${kind}`);}
