import { Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { errorMessage } from '../core/api.service';
import { NativeSourcesStore, NativeConnection, NativeExploration, NativeProfile } from '../core/native-sources.store';

@Component({selector:'app-native-sources',imports:[FormsModule,RouterLink],templateUrl:'./native-sources.component.html',styleUrl:'./native-sources.component.css'})
export class NativeSourcesComponent {
  readonly store=inject(NativeSourcesStore);
  form=viewChild.required<ElementRef<HTMLFormElement>>('form');
  busy=signal(false);error=signal('');message=signal('');result=signal<NativeExploration|null>(null);title=signal('');
  name='';config:NativeConnection={engine:'redis',host:'',port:6379,namespace:'0',username:'',password:'',tls:true,prefix:''};
  private pending?:Subscription;
  constructor(){this.store.load();inject(DestroyRef).onDestroy(()=>{this.pending?.unsubscribe();this.config.password='';});}
  engineChanged(engine:string){this.config={...this.config,engine,port:this.store.connectors().find(c=>c.engine===engine)?.default_port??0,namespace:engine==='redis'?'0':'',prefix:'',password:'',tls:true};this.changed();}
  changed(){this.message.set('');this.error.set('');}
  configured(profile:NativeProfile,explore:boolean){
    if(this.busy())return;
    this.begin();this.title.set(this.store.name(profile.engine)+' · '+profile.namespace);
    this.pending=this.store.profile(profile.id,explore).subscribe({next:response=>{
      if('objects' in response)this.result.set(response);
      this.message.set(explore?'Exploration terminée.':'Connexion réussie.');this.busy.set(false);
    },error:e=>this.failed(e)});
  }
  custom(explore:boolean){
    if(this.busy()||!this.form().nativeElement.reportValidity()||!this.store.connectors().some(c=>c.engine===this.config.engine))return;
    const config={...this.config,host:this.config.host.trim(),namespace:this.config.namespace.trim(),username:this.config.username.trim()};
    this.begin();this.title.set(this.name.trim());
    if(explore)this.pending=this.store.explore(config).subscribe({next:result=>{
      this.result.set(result);this.store.save(this.name.trim(),config,result);this.config.password='';
      this.message.set('Connexion ajoutée pour cette session. Mot de passe effacé.');this.busy.set(false);
    },error:e=>this.failed(e)});
    else this.pending=this.store.test(config).subscribe({next:()=>{this.message.set('Connexion réussie.');this.busy.set(false);},error:e=>this.failed(e)});
  }
  private begin(){this.busy.set(true);this.error.set('');this.message.set('');this.result.set(null);}
  private failed(error:unknown){this.busy.set(false);this.error.set(errorMessage(error));}
  cached(name:string,result:NativeExploration){this.title.set(name);this.result.set(result);this.message.set('Structure conservée pour cette session.');this.error.set('');}
}
