import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { catchError, finalize, forkJoin, of, Subscription } from 'rxjs';
import { diagnosticSteps, SourceTestApi, SourceTestRun } from '../features/connection-test/source-test.api';
import { ApiService, errorMessage } from './api.service';
import { Exploration, Source } from '../../types';

export type SourceState = 'connected'|'failed'|'testing'|'untested';
export interface NativeTestRun {state:'running'|'passed'|'failed';message:string}
export interface SourceEntry extends Source {
  name: string; state: SourceState; testedAt: string|null; error?: string;
  temporary?: boolean; exploration?: Exploration;
  native_profile?:string;
}
export const illustrativeSources: SourceEntry[] = [
  {id:'erp',name:'erp-production',host:'sql-erp-prod.corp.internal',port:1433,database:'ERP_PROD',state:'connected',testedAt:'2026-11-16T09:42:00'},
  {id:'crm',name:'crm-sales',host:'sql-crm-01.corp.internal',port:1433,database:'CRM_Sales',state:'connected',testedAt:'2026-11-16T08:15:00'},
  {id:'finance',name:'finance-dwh',host:'10.24.8.31',port:1433,database:'FinanceDW',state:'failed',testedAt:'2026-11-15T17:03:00',error:'Identifiants refusés par le serveur'},
  {id:'hr',name:'hr-core',host:'sql-hr.corp.internal',port:1433,database:'HR_Core',state:'testing',testedAt:'2026-11-14T11:20:00'},
  {id:'logistics',name:'logistics-wms',host:'sql-wms-02.corp.internal',port:1450,database:'WMS_Live',state:'untested',testedAt:null},
  {id:'retail',name:'retail-pos-archive',host:'sql-pos-arch.corp.internal',port:1433,database:'POS_Archive',state:'connected',testedAt:'2026-11-12T16:48:00'}
].map(s=>({...s,engine:'sqlserver',username:'svc_dataplatform'} as SourceEntry));

@Injectable({providedIn:'root'})
export class SourcesStore {
  private api=inject(ApiService);
  private diagnostics=inject(SourceTestApi);
  testRuns=signal<Record<string,SourceTestRun>>({});
  nativeTests=signal<Record<string,NativeTestRun>>({});
  readonly mockup=new URLSearchParams(location.search).has('maquette');
  rows=signal<SourceEntry[]>([]); loading=signal(false); error=signal('');
  search=signal(''); filter=signal<'all'|'connected'|'failed'|'untested'>('all');
  readonly matching=computed(()=>{const q=this.search().trim().toLocaleLowerCase('fr');return this.rows().filter(s=>[s.name,s.host,s.database].some(v=>v.toLocaleLowerCase('fr').includes(q)));});
  readonly visible=computed(()=>this.matching().filter(s=>this.filter()==='all'||s.state===this.filter()));
  private pending=new Subscription(); private listRequest?:Subscription;
  private testing=new Map<string,Subscription>(); private loaded=false;
  constructor(){inject(DestroyRef).onDestroy(()=>{this.pending.unsubscribe();this.listRequest?.unsubscribe();});}
  load(force=false){
    if(this.loaded&&!force)return;
    this.loaded=true;this.listRequest?.unsubscribe();this.loading.set(true);this.error.set('');
    if(this.mockup){this.rows.set(new URLSearchParams(location.search).get('state')==='empty'?[]:illustrativeSources.map(s=>({...s})));this.loading.set(false);return;}
    const failed=(e:unknown,label='')=>{this.error.update(message=>[message,label+errorMessage(e)].filter(Boolean).join(' '));return of([]);};
    this.listRequest=forkJoin({sources:this.api.sources().pipe(catchError(e=>failed(e))),natives:this.api.nativeSources().pipe(catchError(e=>failed(e,'Sources natives : ')))}).subscribe({next:({sources,natives})=>{
      const configured:Source[]=sources;
      const nativeEntries=natives.map(p=>({id:'native:'+p.id,native_profile:p.id,engine:p.engine,host:p.host,port:p.port,database:p.namespace,username:p.username,name:p.id}));
      const old=this.rows();this.rows.set([...([...configured,...nativeEntries]).map(s=>{const prior=old.find(p=>p.id===s.id&&!p.temporary);return {...s,name:s.id,...('native_profile' in s?{native_profile:s.native_profile,name:s.native_profile}:{}),state:prior?.state??'untested',testedAt:prior?.testedAt??null,error:prior?.error} as SourceEntry;}),...old.filter(s=>s.temporary)]);this.loading.set(false);
    },error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});
  }
  count(state:string){return this.matching().filter(s=>state==='all'||s.state===state).length;}
  test(source:SourceEntry){
    if(source.temporary||this.mockup||this.testing.has(source.id))return;
    this.update(source.id,{state:'testing',error:undefined});
    if(source.native_profile){
      const finish=(passed:boolean,message:string)=>{this.nativeTests.update(runs=>({...runs,[source.id]:{state:passed?'passed':'failed',message}}));this.update(source.id,{state:passed?'connected':'failed',testedAt:new Date().toISOString(),error:passed?undefined:message});};
      this.nativeTests.update(runs=>({...runs,[source.id]:{state:'running',message:'Vérification de l’accès et de l’authentification…'}}));
      const holder=new Subscription();this.testing.set(source.id,holder);this.pending.add(holder);
      holder.add(this.api.testNativeSource(source.native_profile).pipe(finalize(()=>this.testing.delete(source.id))).subscribe({next:result=>finish(result.status==='connected',result.status==='connected'?'Connexion réussie.':'Résultat de connexion non confirmé.'),error:e=>finish(false,errorMessage(e))}));return;
    }
    const steps=Object.fromEntries(diagnosticSteps.map(step=>[step,{state:'waiting',message:'',duration_ms:0}])) as SourceTestRun['steps'];
    this.testRuns.update(runs=>({...runs,[source.id]:{state:'running',steps,message:'Test de connexion en cours…',duration_ms:null}}));
    const holder=new Subscription();this.testing.set(source.id,holder);this.pending.add(holder);
    holder.add(this.diagnostics.progress(source.id).pipe(finalize(()=>this.testing.delete(source.id))).subscribe({next:event=>{
      this.testRuns.update(runs=>{const run=runs[source.id];return {...runs,[source.id]:event.step==='complete'?{...run,state:event.state==='passed'?'passed':'failed',message:event.message,duration_ms:event.duration_ms}:{...run,steps:{...run.steps,[event.step]:{state:event.state,message:event.message,duration_ms:event.duration_ms}}}};});
      if(event.step==='complete')this.update(source.id,{state:event.state==='passed'?'connected':'failed',testedAt:new Date().toISOString(),error:event.state==='passed'?undefined:event.message});
    },error:e=>this.interrupted(source.id,errorMessage(e)),complete:()=>{if(this.testRuns()[source.id]?.state==='running')this.interrupted(source.id,'Le suivi a été interrompu : résultat non confirmé.');}}));
  }
  private interrupted(id:string,message:string){this.testRuns.update(runs=>{const run=runs[id];return {...runs,[id]:{...run,state:'failed',message,steps:Object.fromEntries(Object.entries(run.steps).map(([key,step])=>[key,step.state==='running'?{...step,state:'unknown'}:step])) as SourceTestRun['steps']}};});this.update(id,{state:'failed',testedAt:new Date().toISOString(),error:message});}
  addSession(source:Source,name:string,exploration:Exploration){
    // Keep schema metadata in memory only. Credentials never enter this store.
    const safe:SourceEntry={id:source.id,engine:source.engine,host:source.host,port:source.port,database:source.database,username:source.username,name:name.trim(),state:'connected',testedAt:new Date().toISOString(),temporary:true,exploration};
    this.rows.update(rows=>[...rows,safe]);this.loaded=true;return safe;
  }
  private update(id:string,changes:Partial<SourceEntry>){this.rows.update(rows=>rows.map(s=>s.id===id?{...s,...changes}:s));}
}
