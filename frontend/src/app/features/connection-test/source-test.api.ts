import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthStore } from '../auth/auth.store';
import { safeReturnUrl } from '../auth/auth.guard';
export const diagnosticSteps=['dns','tcp','jdbc','validation'] as const;
export type StepId=typeof diagnosticSteps[number];
export interface DiagnosticEvent {step:StepId|'complete';state:'running'|'passed'|'failed'|'skipped';message:string;duration_ms:number}
export interface DiagnosticStep {state:DiagnosticEvent['state']|'waiting'|'unknown';message:string;duration_ms:number}
export interface SourceTestRun {state:'running'|'passed'|'failed';steps:Record<StepId,DiagnosticStep>;message:string;duration_ms:number|null}
export function diagnosticFrame(frame:string):DiagnosticEvent|null {
  const data=frame.split(/\r?\n/).filter(line=>line.startsWith('data:')).map(line=>line.slice(5).trimStart()).join('\n');if(!data)return null;
  const event=JSON.parse(data) as DiagnosticEvent;
  if(![...diagnosticSteps,'complete'].includes(event.step)||!['running','passed','failed','skipped'].includes(event.state)||typeof event.message!=='string'||event.message.length>512||!Number.isFinite(event.duration_ms)||event.duration_ms<0)throw new Error('Réponse de diagnostic invalide.');
  return event;
}
export async function readDiagnostics(body:ReadableStream<Uint8Array>,emit:(event:DiagnosticEvent)=>void){
  const reader=body.getReader(),decoder=new TextDecoder();let buffer='';
  try{while(true){const chunk=await reader.read();buffer+=decoder.decode(chunk.value,{stream:!chunk.done});if(buffer.length>262144)throw new Error('Réponse de diagnostic invalide.');let match;
    while((match=/\r?\n\r?\n/.exec(buffer))){const frame=buffer.slice(0,match.index);buffer=buffer.slice(match.index+match[0].length);const event=diagnosticFrame(frame);if(event)emit(event);}
    if(chunk.done){if(buffer.trim()){const event=diagnosticFrame(buffer);if(event)emit(event);}break;}
  }}finally{reader.releaseLock();}
}
@Injectable({providedIn:'root'})
export class SourceTestApi {
  private auth=inject(AuthStore);private router=inject(Router);
  progress(id:string){return new Observable<DiagnosticEvent>(subscriber=>{
    const abort=new AbortController(),timeout=setTimeout(()=>abort.abort(),90000);
    void(async()=>{
      try{
        const response=await fetch(`/api/sources/${encodeURIComponent(id)}/test/events`,{method:'POST',credentials:'same-origin',headers:{Accept:'text/event-stream, application/json','X-CSRF-TOKEN':this.auth.csrf()},signal:abort.signal});
        if(!response.ok){let error={detail:'Le suivi du test est indisponible.'};try{error=await response.json();}catch{/* Keep the safe generic error. */}
          if(response.status===401){this.auth.clear();void this.router.navigate(['/login'],{queryParams:{returnUrl:safeReturnUrl(this.router.url)}});}throw {status:response.status,error};}
        if(!response.headers.get('Content-Type')?.startsWith('text/event-stream')||!response.body)throw new Error('Suivi du test indisponible.');
        await readDiagnostics(response.body,event=>subscriber.next(event));subscriber.complete();
      }catch(error){if(!subscriber.closed)subscriber.error(error);}finally{clearTimeout(timeout);}
    })();return()=>{clearTimeout(timeout);abort.abort();};
  });}
}
