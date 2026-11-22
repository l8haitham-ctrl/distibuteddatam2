import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { timeout } from 'rxjs';
import { errorMessage } from './api.service';

export interface NativeConnector { engine:string; name:string; default_port:number; capabilities:string[] }
export interface NativeConnection { engine:string; host:string; port:number; namespace:string; username:string; password:string; tls:boolean; prefix:string }
export type NativeProfile=Omit<NativeConnection,'password'> & {id:string};
export interface NativeObject {name:string; type:string; fields:string[]; size?:number}
export interface NativeExploration {engine:string; namespace:string; objects:NativeObject[]; truncated:boolean; limit:number; transport:string; note:string}
export interface NativeSession {id:string; name:string; source:Omit<NativeConnection,'password'>; exploration:NativeExploration}

@Injectable({providedIn:'root'})
export class NativeSourcesStore {
  private http=inject(HttpClient);
  connectors=signal<NativeConnector[]>([]);profiles=signal<NativeProfile[]>([]);sessions=signal<NativeSession[]>([]);
  error=signal('');loading=signal(false);
  load(){
    if(this.loading())return;
    this.loading.set(true);this.error.set('');
    this.http.get<NativeConnector[]>('/api/native/connectors').pipe(timeout(15000)).subscribe({next:connectors=>{
      this.connectors.set(connectors);
      this.http.get<NativeProfile[]>('/api/native/sources').pipe(timeout(15000)).subscribe({next:profiles=>{this.profiles.set(profiles);this.loading.set(false);},error:e=>this.failed(e)});
    },error:e=>this.failed(e)});
  }
  private failed(error:unknown){this.loading.set(false);this.error.set(errorMessage(error));}
  name(engine:string){return this.connectors().find(c=>c.engine===engine)?.name??engine;}
  test(config:NativeConnection){return this.http.post<{status:string}>('/api/native/connections/test',config).pipe(timeout(20000));}
  explore(config:NativeConnection){return this.http.post<NativeExploration>('/api/native/connections/explore',config).pipe(timeout(30000));}
  profile(id:string,explore:boolean){return this.http.post<NativeExploration|{status:string}>(`/api/native/sources/${encodeURIComponent(id)}/${explore?'explore':'test'}`,{}).pipe(timeout(30000));}
  save(name:string,config:NativeConnection,exploration:NativeExploration){
    const {password:_,...source}=config;
    this.sessions.update(rows=>[...rows,{id:crypto.randomUUID(),name,source,exploration}]);
  }
}
