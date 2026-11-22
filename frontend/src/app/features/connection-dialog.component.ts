import { AfterViewInit, Component, ElementRef, inject, input, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Subscription, finalize } from 'rxjs';
import { ApiService, errorMessage } from '../core/api.service';
import { IconComponent } from '../shared/icon.component';
import type { ConnectionConfig, Exploration, Source } from '../../types';
import { ConnectorCatalog } from '../core/connector-catalog';

@Component({selector:'app-connection-dialog',imports:[FormsModule,IconComponent],templateUrl:'./connection-dialog.component.html'})
export class ConnectionDialogComponent implements OnInit, AfterViewInit, OnDestroy {
  defaults = input<Source>(); mockup = input(false); closed = output(); connected = output<{source:Source;data:Exploration}>();
  private api = inject(ApiService); private pending?: Subscription;
  readonly connectors=inject(ConnectorCatalog);
  dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  nativeForm = viewChild.required<ElementRef<HTMLFormElement>>('nativeForm');
  visible = signal(false); busy = signal<'test'|'connect'|null>(null); error = signal(''); tested = signal(false);
  config: ConnectionConfig = {engine:'sqlserver',host:'sqlserver',port:1433,database:'BusinessDB',username:'lake_reader',password:'',trust_server_certificate:true};
  ngOnInit() {
    this.connectors.load();
    const defaults = this.defaults();
    if(defaults) { const {engine,host,port,database,username} = defaults; this.config = {...this.config,engine,host,port,database,username}; }
  }
  ngAfterViewInit() { this.dialog().nativeElement.showModal(); }
  ngOnDestroy(){this.pending?.unsubscribe(); this.config.password = '';}
  close(){this.pending?.unsubscribe();this.config.password='';this.closed.emit();}
  changed(){this.tested.set(false);this.error.set('');}
  engineChanged(engine:string){this.config.engine=engine;this.config.port=this.connectors.get(engine)?.default_port??0;this.config.password='';this.config.trust_server_certificate=false;this.changed();}
  run(operation:'test'|'connect',form:NgForm){
    if(!this.nativeForm().nativeElement.reportValidity()||form.invalid||this.busy())return;
    if(this.mockup()){this.error.set('Maquette illustrative : ouvrez l’application pour tester une connexion réelle.');return;}
    if(!this.connectors.canConnect(this.config.engine)){this.error.set('Ce connecteur n’est pas disponible.');return;}
    const config={...this.config,host:this.config.host.trim(),database:this.config.database.trim(),username:this.config.username.trim()};
    this.busy.set(operation);this.error.set('');this.tested.set(false);
    if(operation==='test') this.pending=this.api.test(config).pipe(finalize(()=>this.busy.set(null))).subscribe({next:()=>this.tested.set(true),error:e=>this.error.set(errorMessage(e))});
    else this.pending=this.api.explore(config).pipe(finalize(()=>this.busy.set(null))).subscribe({next:data=>{
      const {password:_password,trust_server_certificate:_trust,...safe}=config;
      this.config.password='';this.connected.emit({source:{...safe,id:'session'},data});this.closed.emit();
    },error:e=>this.error.set(errorMessage(e))});
  }
}
