import { Component, DestroyRef, ElementRef, Injector, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService, errorMessage } from '../core/api.service';
import { ExplorerStore } from '../core/explorer.store';
import { SourcesStore } from '../core/sources.store';
import { IconComponent } from '../shared/icon.component';
import { ConnectionConfig } from '../../types';

@Component({selector:'app-source-form',imports:[FormsModule,RouterLink,IconComponent],templateUrl:'./source-form.component.html'})
export class SourceFormComponent {
  readonly sources=inject(SourcesStore);private api=inject(ApiService);private router=inject(Router);private injector=inject(Injector);
  nativeForm=viewChild.required<ElementRef<HTMLFormElement>>('form');
  readonly mockup=this.sources.mockup;
  state=signal<'initial'|'testing'|'success'|'error'>('initial'); busy=signal(false); saving=signal(false);
  error=signal(''); visible=signal(false); advanced=signal(true); elapsed=signal(0); testedAt=signal<Date|null>(null);
  name=''; timeoutSeconds=30;
  config:ConnectionConfig={engine:'sqlserver',host:'',port:1433,database:'',username:'',password:'',trust_server_certificate:false};
  private pending?:Subscription;
  constructor(){
    this.sources.load();
    if(this.mockup){
      this.name='erp-production';this.config={...this.config,host:'sql-erp-prod.corp.internal',database:'ERP_PROD',username:'svc_dataplatform',password:'illustrative-only'};
      const state=new URLSearchParams(location.search).get('state');
      if(state==='success'||state==='testing'||state==='error'){this.state.set(state);this.advanced.set(false);this.elapsed.set(state==='error'?1200:38);this.testedAt.set(new Date(2026,10,16,16,45));}
      if(state==='testing')this.busy.set(true);
      if(state==='error')this.error.set('Le serveur a été joint, mais il a refusé le nom d’utilisateur ou le mot de passe.');
    }
    inject(DestroyRef).onDestroy(()=>{this.pending?.unsubscribe();this.config.password='';});
  }
  changed(){if(!this.busy()){this.state.set('initial');this.testedAt.set(null);this.error.set('');}}
  valid(){return this.name.trim().length>0&&(this.mockup||!this.sources.rows().some(s=>s.name.toLocaleLowerCase()===this.name.trim().toLocaleLowerCase()))&&Number.isInteger(this.timeoutSeconds)&&this.timeoutSeconds>=1&&this.timeoutSeconds<=120;}
  run(save=false){
    if(this.busy()||!this.nativeForm().nativeElement.reportValidity()||!this.valid())return;
    if(this.mockup){this.error.set('Maquette illustrative : les actions réseau sont désactivées.');return;}
    const config={...this.config,host:this.config.host.trim(),database:this.config.database.trim(),username:this.config.username.trim()};
    this.error.set('');this.busy.set(true);this.saving.set(save);this.state.set('testing');this.advanced.set(false);
    const started=performance.now();
    if(save)this.pending=this.api.explore(config).subscribe({next:data=>{
      const source={id:'session-'+crypto.randomUUID(),engine:config.engine,host:config.host,port:config.port,database:config.database,username:config.username};
      this.sources.addSession(source,this.name,data);this.injector.get(ExplorerStore).apply(source,data);
      this.config.password='';this.busy.set(false);this.saving.set(false);void this.router.navigate(['/sources']);
    },error:e=>this.failed(e,started)});
    else this.pending=this.api.test(config,this.timeoutSeconds*1000).subscribe({next:()=>{
      this.elapsed.set(Math.round(performance.now()-started));this.testedAt.set(new Date());this.state.set('success');this.busy.set(false);
    },error:e=>this.failed(e,started)});
  }
  cancelTest(){this.pending?.unsubscribe();this.busy.set(false);this.saving.set(false);this.state.set('initial');this.error.set('');}
  private failed(e:unknown,started:number){this.elapsed.set(Math.round(performance.now()-started));this.testedAt.set(new Date());this.error.set(errorMessage(e));this.state.set('error');this.busy.set(false);this.saving.set(false);}
  get authenticationError(){return /identifiant|authentifi|mot de passe/i.test(this.error());}
  stages=['Joindre le serveur','Sécuriser la connexion','S’authentifier','Ouvrir la base de données'];
  stageNote(index:number){return [this.config.host+':'+this.config.port,this.stageState(1)==='done'?'Chiffrée (TLS)':'TLS requis',this.state()==='error'&&this.authenticationError?'Identifiants refusés pour '+this.config.username:'En tant que '+this.config.username,this.config.database][index];}
  stageState(index:number){
    if(this.state()==='success')return 'done';
    if(this.state()==='error'&&this.authenticationError)return index<2?'done':index===2?'failed':'skipped';
    if(this.mockup&&this.state()==='testing')return index<2?'done':index===2?'active':'waiting';
    return 'waiting';
  }
  stageLabel(index:number){if(this.state()==='error'&&!this.authenticationError)return 'Non confirmé';return ({done:'Réussi',failed:'Échec',skipped:'Ignoré',active:'En cours',waiting:'En attente'})[this.stageState(index)];}
  formattedDate(){return this.testedAt()?new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}).format(this.testedAt()!):'';}
}
