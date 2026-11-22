import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ApiService, errorMessage } from './api.service';
import { Exploration, Source } from '../../types';

export type SourceState = 'connected'|'failed'|'testing'|'untested';
export interface SourceEntry extends Source {
  name: string; state: SourceState; testedAt: string|null; error?: string;
  temporary?: boolean; exploration?: Exploration;
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
    this.listRequest=this.api.sources().subscribe({next:sources=>{
      const old=this.rows();this.rows.set([...sources.map(s=>{const prior=old.find(p=>p.id===s.id&&!p.temporary);return {...s,name:s.id,state:prior?.state??'untested',testedAt:prior?.testedAt??null,error:prior?.error} as SourceEntry;}),...old.filter(s=>s.temporary)]);this.loading.set(false);
    },error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});
  }
  count(state:string){return this.matching().filter(s=>state==='all'||s.state===state).length;}
  test(source:SourceEntry){
    if(source.temporary||this.mockup||this.testing.has(source.id))return;
    this.update(source.id,{state:'testing',error:undefined});
    const request=this.api.testSource(source.id).subscribe({next:()=>{this.update(source.id,{state:'connected',testedAt:new Date().toISOString()});this.testing.delete(source.id);},error:e=>{this.update(source.id,{state:'failed',testedAt:new Date().toISOString(),error:errorMessage(e)});this.testing.delete(source.id);}});
    this.testing.set(source.id,request);this.pending.add(request);
  }
  addSession(source:Source,name:string,exploration:Exploration){
    // Keep schema metadata in memory only. Credentials never enter this store.
    const safe:SourceEntry={id:source.id,engine:source.engine,host:source.host,port:source.port,database:source.database,username:source.username,name:name.trim(),state:'connected',testedAt:new Date().toISOString(),temporary:true,exploration};
    this.rows.update(rows=>[...rows,safe]);this.loaded=true;return safe;
  }
  private update(id:string,changes:Partial<SourceEntry>){this.rows.update(rows=>rows.map(s=>s.id===id?{...s,...changes}:s));}
}
