import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ApiService, errorMessage } from './api.service';
import { ConnectorInfo } from '../../types';

@Injectable({providedIn:'root'})
export class ConnectorCatalog {
  private api=inject(ApiService); private request?:Subscription; private loaded=false;
  readonly mockup=new URLSearchParams(location.search).has('maquette');
  entries=signal<ConnectorInfo[]>([]);loading=signal(false);error=signal('');
  constructor(){inject(DestroyRef).onDestroy(()=>this.request?.unsubscribe());}
  load(force=false){
    if(this.loaded&&!force)return;this.loaded=true;this.request?.unsubscribe();this.error.set('');this.loading.set(true);
    if(this.mockup){this.entries.set([{engine:'sqlserver',name:'Microsoft SQL Server',default_port:1433,default_schema:'dbo',available:true,capabilities:['connection','exploration','extraction']}]);this.loading.set(false);return;}
    this.request=this.api.connectors().subscribe({next:entries=>{this.entries.set(entries);this.loading.set(false);},error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});
  }
  get(engine:string){return this.entries().find(e=>e.engine===engine);}
  name(engine:string){return this.get(engine)?.name ?? engine;}
  shortName(engine:string){return this.name(engine).replace(/^Microsoft /,'');}
  canConnect(engine:string){const connector=this.get(engine);return !this.loading()&&!this.error()&&!!connector?.available&&connector.capabilities.includes('connection');}
}
