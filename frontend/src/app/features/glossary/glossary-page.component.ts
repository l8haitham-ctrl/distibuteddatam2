import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import { IconComponent } from '../../shared/icon.component';
import { Source, TableDetails, TableRef } from '../../../types';
import { GlossaryApi, Target, Term, TermDraft } from './glossary.api';
const empty=():TermDraft=>({name:'',definition:'',calculation_rule:'',owner:'',status:'DRAFT'});
@Component({selector:'app-glossary-page',imports:[FormsModule,DatePipe,RouterLink,IconComponent],templateUrl:'./glossary-page.component.html',styleUrl:'./glossary.css'})
export class GlossaryPageComponent {
  private api=inject(GlossaryApi);private sourcesApi=inject(ApiService);private router=inject(Router);private route=inject(ActivatedRoute);
  terms=signal<Term[]>([]);term=signal<Term|null>(null);editing=signal(false);draft:TermDraft=empty();
  loading=signal(false);detailLoading=signal(false);busy=signal(false);error=signal('');listError=signal('');message=signal('');
  context=signal('');
  sources=signal<Source[]>([]);tables=signal<TableRef[]>([]);columns=signal<TableDetails['columns']>([]);
  targetLoading=signal(false);targetError=signal('');sourceId='';tableKey='';column='';q='';offset=0;
  private listRequest?:Subscription;private detailRequest?:Subscription;private tablesRequest?:Subscription;private columnsRequest?:Subscription;
  private pending=new Subscription();private wanted:Target|null=null;
  private openedId='';
  constructor(){
    inject(DestroyRef).onDestroy(()=>{this.listRequest?.unsubscribe();this.detailRequest?.unsubscribe();this.tablesRequest?.unsubscribe();this.columnsRequest?.unsubscribe();this.pending.unsubscribe();});
    this.loadSources();
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(p=>{
      const source=p.get('source_id'),schema=p.get('schema_name'),table=p.get('table_name');
      this.wanted=source&&schema&&table?{source_id:source,schema_name:schema,table_name:table,column_name:p.get('column_name')}:null;
      this.context.set(this.wanted?`${source} / ${schema}.${table}${this.wanted.column_name?' / '+this.wanted.column_name:''}`:'');
      if(this.wanted&&this.sources().length)this.sourceChanged(this.wanted.source_id,this.wanted);
      const id=p.get('term');if(id)this.open(id);else{this.term.set(null);this.editing.set(false);}
    });this.search();
  }
  search(reset=true){if(reset)this.offset=0;this.listRequest?.unsubscribe();this.loading.set(true);this.listError.set('');
    this.listRequest=this.api.list(this.q,this.offset).subscribe({next:rows=>{this.terms.set(rows);this.loading.set(false);},error:e=>{this.loading.set(false);this.listError.set(errorMessage(e));}});}
  page(delta:number){this.offset=Math.max(0,this.offset+delta*20);this.search(false);}
  choose(id:string){if(this.busy())return;if(this.openedId===id)this.open(id);else void this.router.navigate(['/glossary'],{queryParams:{term:id},queryParamsHandling:'merge'});}
  loadSources(){this.targetError.set('');this.pending.add(this.sourcesApi.sources().subscribe({next:s=>{this.sources.set(s);if(this.wanted)this.sourceChanged(this.wanted.source_id,this.wanted);},error:e=>this.targetError.set(errorMessage(e))}));}
  retryTarget(){if(!this.sources().length)this.loadSources();else this.sourceChanged(this.sourceId);}
  open(id:string){this.openedId=id;this.detailRequest?.unsubscribe();this.term.set(null);this.editing.set(false);this.error.set('');this.message.set('');this.detailLoading.set(true);
    this.detailRequest=this.api.get(id).subscribe({next:t=>{this.term.set(t);this.detailLoading.set(false);},error:e=>{this.error.set(errorMessage(e));this.detailLoading.set(false);}});}
  create(){if(this.busy())return;this.detailRequest?.unsubscribe();this.detailLoading.set(false);this.term.set(null);this.draft=empty();this.editing.set(true);this.error.set('');this.message.set('');}
  edit(){const t=this.term();if(!t)return;this.draft={name:t.name,definition:t.definition,calculation_rule:t.calculation_rule,owner:t.owner,status:t.status};this.editing.set(true);this.error.set('');}
  save(){if(this.busy()||!this.draft.name.trim()||!this.draft.definition.trim())return;this.busy.set(true);this.error.set('');
    this.pending.add(this.api.save({...this.draft},this.term()?.id).subscribe({next:t=>{this.term.set(t);this.editing.set(false);this.busy.set(false);this.message.set('Terme enregistré.');this.search();void this.router.navigate(['/glossary'],{queryParams:{term:t.id},queryParamsHandling:'merge'});},error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}}));}
  sourceChanged(id:string,prefill?:Target){this.tablesRequest?.unsubscribe();this.columnsRequest?.unsubscribe();this.sourceId=id;this.tableKey='';this.column='';this.tables.set([]);this.columns.set([]);this.targetError.set('');this.targetLoading.set(false);
    if(!id)return;if(!this.sources().some(s=>s.id===id)){this.targetError.set('Source configurée introuvable.');return;}this.targetLoading.set(true);
    this.tablesRequest=this.sourcesApi.tables(id).subscribe({next:r=>{this.tables.set(r.tables);this.targetLoading.set(false);if(prefill){const t=r.tables.find(t=>t.schema_name===prefill.schema_name&&t.table_name===prefill.table_name);if(t)this.tableChanged(this.key(t),prefill.column_name??'');else this.targetError.set('La table liée n’est plus accessible.');}},error:e=>{this.targetLoading.set(false);this.targetError.set(errorMessage(e));}});}
  key(t:TableRef){return JSON.stringify([t.schema_name,t.table_name]);}
  selectedTable(){return this.tables().find(t=>this.key(t)===this.tableKey);}
  tableChanged(key:string,prefill=''){this.columnsRequest?.unsubscribe();this.tableKey=key;this.column='';this.columns.set([]);this.targetError.set('');this.targetLoading.set(false);const t=this.selectedTable();if(!t)return;this.targetLoading.set(true);
    this.columnsRequest=this.sourcesApi.describe(this.sourceId,t).subscribe({next:r=>{this.columns.set(r.columns);this.targetLoading.set(false);if(prefill){if(r.columns.some(c=>c.name===prefill))this.column=prefill;else this.targetError.set('La colonne liée n’est plus accessible.');}},error:e=>{this.targetLoading.set(false);this.targetError.set(errorMessage(e));}});}
  addLink(){const t=this.selectedTable(),term=this.term();if(!t||!term||this.busy()||this.targetLoading()||this.targetError())return;this.busy.set(true);this.error.set('');this.message.set('');
    this.pending.add(this.api.link(term.id,{source_id:this.sourceId,...t,column_name:this.column||null}).subscribe({next:link=>{this.term.update(current=>current?{...current,links:[...(current.links??[]),link]}:current);this.busy.set(false);this.message.set('Lien ajouté et vérifié sur la source.');},error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}}));}
  removeLink(link:string){const term=this.term();if(!term||this.busy())return;this.busy.set(true);this.error.set('');this.pending.add(this.api.unlink(term.id,link).subscribe({next:()=>{this.term.update(t=>t?{...t,links:t.links?.filter(l=>l.id!==link)??[]}:t);this.busy.set(false);this.message.set('Lien retiré du glossaire.');},error:e=>{this.busy.set(false);this.error.set(errorMessage(e));}}));}
}
