import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { errorMessage } from '../core/api.service';
import { NativeSourcesStore, NativeExploration, NativeObject, NativeProfile } from '../core/native-sources.store';
import { PipelineApi } from '../core/pipeline.api';
import { IconComponent } from '../shared/icon.component';
import { TableTreeComponent } from './table-tree.component';
import { TableRef } from '../../types';

@Component({selector:'app-native-explorer',imports:[RouterLink,IconComponent,TableTreeComponent],templateUrl:'./native-explorer.component.html',styleUrl:'./native-explorer.component.css'})
export class NativeExplorerComponent {
  readonly store=inject(NativeSourcesStore);
  private route=inject(ActivatedRoute);private router=inject(Router);private pipeline=inject(PipelineApi);
  private params=toSignal(this.route.queryParamMap,{initialValue:this.route.snapshot.queryParamMap});
  private request?:Subscription;private submission?:Subscription;
  activeProfile=signal<NativeProfile|null>(null);result=signal<NativeExploration|null>(null);
  busy=signal(false);error=signal('');extractionError=signal('');extracting=signal(false);uncertain=signal(false);
  selected=signal<NativeObject|null>(null);tab=signal<'structure'|'properties'>('structure');
  itemKind=computed(()=>'redis'===this.result()?.engine?'key' as const:'minio'===this.result()?.engine?'file' as const:'table' as const);
  itemsLabel=computed(()=>({table:'tables',key:'clés',file:'fichiers'})[this.itemKind()]);
  group=computed(()=>this.result()?.engine==='redis'?'Clés Redis':this.result()?.engine==='minio'?'Fichiers':this.result()?.namespace??'');
  tables=computed<TableRef[]>(()=>this.result()?.objects.map(o=>({schema_name:this.group(),table_name:o.name}))??[]);
  selectedRef=computed(()=>this.selected()?{schema_name:this.group(),table_name:this.selected()!.name}:null);
  columns=computed(()=>this.selected()?.columns??[]);
  constructor(){
    this.store.load();
    effect(()=>{
      const id=this.params().get('source_id')?.replace(/^native:/,'');
      const loading=this.store.loading(),profiles=this.store.profiles(),failure=this.store.error();
      if(loading)return;
      untracked(()=>{
        const profile=profiles.find(p=>p.id===id);
        if(profile)this.explore(profile);
        else{this.request?.unsubscribe();this.activeProfile.set(null);this.result.set(null);this.selected.set(null);this.busy.set(false);this.error.set(failure||'Source indisponible. Choisissez une source dans la liste.');}
      });
    });
    inject(DestroyRef).onDestroy(()=>{this.request?.unsubscribe();this.submission?.unsubscribe();});
  }
  explore(profile:NativeProfile){
    if(this.extracting())return;
    const previous=this.activeProfile()?.id===profile.id?this.selected()?.name:undefined;
    this.request?.unsubscribe();this.activeProfile.set(profile);this.result.set(null);this.selected.set(null);
    this.busy.set(true);this.error.set('');if(!this.uncertain())this.extractionError.set('');
    this.request=this.store.profile(profile.id,true).subscribe({next:response=>{
      if(!('objects' in response)){this.busy.set(false);this.error.set('Structure non disponible.');return;}
      this.result.set(response);this.select(response.objects.find(o=>o.name===previous)??response.objects[0]??null);this.busy.set(false);
    },error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}});
  }
  refresh(){const profile=this.activeProfile();if(profile&&!this.busy())this.explore(profile);else if(!profile)this.store.load();}
  select(object:NativeObject|null){this.selected.set(object);this.tab.set('structure');}
  selectTable(table:TableRef){this.select(this.result()?.objects.find(o=>o.name===table.table_name)??null);}
  tabKey(event:KeyboardEvent){
    if(!['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;
    event.preventDefault();const tab=event.key==='Home'?'structure':event.key==='End'?'properties':this.tab()==='structure'?'properties':'structure';
    this.tab.set(tab);document.getElementById('native-tab-'+tab)?.focus();
  }
  extract(){
    const profile=this.activeProfile(),selected=this.selected();
    if(!this.result()||!selected||!profile?.extraction_source_id||this.extracting()||this.uncertain()||this.busy())return;
    const table=profile.extraction_dataset??selected.name;
    this.extracting.set(true);this.extractionError.set('');
    this.submission=this.pipeline.create({source_id:profile.extraction_source_id,schema:profile.extraction_schema??profile.namespace,table,format:'parquet',compression:'snappy',partition_by:[],partition_granularity:'month',fetch_size:1000,num_partitions:1}).subscribe({
      next:job=>{void this.router.navigate(['/jobs',job.id]);},
      error:error=>{this.extracting.set(false);const code=(error as {status?:number}).status;if(!code||![400,404,422].includes(code))this.uncertain.set(true);this.extractionError.set(errorMessage(error)+(this.uncertain()?' Soumission non confirmée : vérifiez Storage Jobs avant toute relance.':''));}
    });
  }
}
