import { Component, DestroyRef, ElementRef, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { errorMessage } from '../core/api.service';
import { NativeSourcesStore, NativeConnection, NativeExploration, NativeProfile } from '../core/native-sources.store';
import { PipelineApi } from '../core/pipeline.api';

@Component({selector:'app-native-sources',imports:[FormsModule,RouterLink],templateUrl:'./native-sources.component.html',styleUrl:'./native-sources.component.css'})
export class NativeSourcesComponent {
  readonly store=inject(NativeSourcesStore);
  form=viewChild.required<ElementRef<HTMLFormElement>>('form');
  busy=signal(false);error=signal('');message=signal('');result=signal<NativeExploration|null>(null);title=signal('');
  name='';config:NativeConnection={engine:'redis',host:'',port:6379,namespace:'0',username:'',password:'',tls:true,prefix:''};
  private pending?:Subscription;
  private pipeline=inject(PipelineApi);private router=inject(Router);private submission?:Subscription;
  activeProfile=signal<NativeProfile|null>(null);extracting=signal(false);uncertain=signal(false);
  constructor(){
    const requested=inject(ActivatedRoute).snapshot.queryParamMap.get('source_id');let opened=false;
    this.store.load();
    effect(()=>{if(!requested||opened||this.store.loading())return;const profile=this.store.profiles().find(p=>p.id===requested);if(profile){opened=true;untracked(()=>this.configured(profile,true));}else if(!this.store.error()){this.error.set('Source native indisponible.');}});
    inject(DestroyRef).onDestroy(()=>{this.pending?.unsubscribe();this.submission?.unsubscribe();this.config.password='';});
  }
  engineChanged(engine:string){this.config={...this.config,engine,port:this.store.connectors().find(c=>c.engine===engine)?.default_port??0,namespace:engine==='redis'?'0':'',prefix:'',password:'',tls:true};this.changed();}
  changed(){this.message.set('');this.error.set('');}
  configured(profile:NativeProfile,explore:boolean){
    if(this.busy()||this.extracting())return;
    this.begin();this.title.set(this.store.name(profile.engine)+' · '+profile.namespace);
    this.pending=this.store.profile(profile.id,explore).subscribe({next:response=>{
      if('objects' in response){this.result.set(response);this.activeProfile.set(profile);}
      this.message.set(explore?'Exploration terminée.':'Connexion réussie.');this.busy.set(false);
    },error:e=>this.failed(e)});
  }
  custom(explore:boolean){
    if(this.busy()||this.extracting()||!this.form().nativeElement.reportValidity()||!this.store.connectors().some(c=>c.engine===this.config.engine))return;
    const config={...this.config,host:this.config.host.trim(),namespace:this.config.namespace.trim(),username:this.config.username.trim()};
    this.begin();this.title.set(this.name.trim());
    if(explore)this.pending=this.store.explore(config).subscribe({next:result=>{
      this.result.set(result);this.store.save(this.name.trim(),config,result);this.config.password='';
      this.message.set('Connexion ajoutée pour cette session. Mot de passe effacé.');this.busy.set(false);
    },error:e=>this.failed(e)});
    else this.pending=this.store.test(config).subscribe({next:()=>{this.message.set('Connexion réussie.');this.busy.set(false);},error:e=>this.failed(e)});
  }
  extract(table:string){
    const profile=this.activeProfile();if(!profile?.extraction_source_id||this.extracting()||this.uncertain()||this.busy())return;
    this.extracting.set(true);this.error.set('');
    this.submission=this.pipeline.create({source_id:profile.extraction_source_id,schema:profile.extraction_schema??profile.namespace,table,format:'parquet',compression:'snappy',partition_by:[],partition_granularity:'month',fetch_size:1000,num_partitions:1}).subscribe({
      next:job=>{this.message.set('Extraction créée.');void this.router.navigate(['/jobs',job.id]);},
      error:error=>{this.extracting.set(false);const code=(error as {status?:number}).status;if(!code||![400,404,422].includes(code))this.uncertain.set(true);this.error.set(errorMessage(error)+(this.uncertain()?' Soumission non confirmée : vérifiez Storage Jobs avant toute relance.':''));}
    });
  }
  private begin(){this.busy.set(true);this.error.set('');this.message.set('');this.result.set(null);this.activeProfile.set(null);}
  private failed(error:unknown){this.busy.set(false);this.error.set(errorMessage(error));}
  cached(name:string,result:NativeExploration){if(this.extracting())return;this.activeProfile.set(null);this.title.set(name);this.result.set(result);this.message.set('Structure conservée pour cette session.');this.error.set('');}
}
