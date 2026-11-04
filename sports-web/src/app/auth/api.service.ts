import { Injectable } from '@angular/core';
@Injectable({providedIn:'root'})
export class ApiService {
  base=sessionStorage.getItem('sports.api')||'/api/sports/v1';
  token=sessionStorage.getItem('sports.token')||'';
  connected=true;
  async request<T>(path:string,method='GET',body?:unknown):Promise<T>{
    const abort=new AbortController(); const timer=setTimeout(()=>abort.abort(),15000);
    try {
      const response=await fetch(this.base+path,{method,signal:abort.signal,cache:'no-store',credentials:'omit',headers:{'Content-Type':'application/json',...(this.token?{Authorization:`Bearer ${this.token}`}:{})},body:body===undefined?undefined:JSON.stringify(body)});
      this.connected=true;
      const text=await response.text(); let data:any;
      try { data=text?JSON.parse(text):undefined; } catch { throw new Error('Respuesta inesperada del servidor'); }
      if(!response.ok){if(response.status===401)this.clear(); throw Object.assign(new Error(data?.message||'No pudimos completar la operación'),{code:data?.code,status:response.status});}
      return data as T;
    }catch(error:any){if(error.name==='AbortError'||error instanceof TypeError){this.connected=false;throw new Error('Sin conexión: no podemos confirmar que se guardó. Actualiza antes de reintentar.');}throw error;}finally{clearTimeout(timer);}
  }
  save(token:string){this.token=token;sessionStorage.setItem('sports.token',token);sessionStorage.setItem('sports.api',this.base);}
  clear(){this.token='';sessionStorage.removeItem('sports.token');}
}
