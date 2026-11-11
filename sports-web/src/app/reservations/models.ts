export interface Actor {id:string; tenantId:string; name:string; role:'ADMIN'|'EMPLOYEE'}
export interface Court {id:string; name:string; sport:string; capacity:number; durationMinutes:number; color:string; active:boolean}
export interface Contact {id:string; name:string; phone?:string; notes?:string}
export interface Booking {id:string; recurrenceId?:string; courtId:string; guestName:string; teamName?:string; phone?:string; clientId?:string; teacherId?:string; kind:string; status:string; start:string; end:string; notes?:string; price?:number; rewardPercent?:number; finalPrice?:number; version:number; actualStartedAt?:string; actualEndedAt?:string; waterDeliveredAt?:string; soundDevice?:string}
export interface Settings {name:string; timezone:string; currency:string; toleranceMinutes:number; waterMinutes:number; warningMinutes:number}
export interface Operations {serverTime:string; settings:Settings; courts:Court[]; active:Booking[]; reservations:Booking[]}
export const labels:Record<string,string>={PENDING:'Por confirmar',CONFIRMED:'Confirmada',IN_PROGRESS:'En juego',FINISHED:'Finalizada',CANCELLED_CLIENT:'Cancelada',CANCELLED_RAIN:'Lluvia',NO_SHOW:'No asistió',NORMAL:'Normal',FIXED:'Fija',CLASS:'Escuelita',BLOCK:'Bloqueo'};
