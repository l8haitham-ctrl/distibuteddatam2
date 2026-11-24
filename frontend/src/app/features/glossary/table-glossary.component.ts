import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, switchMap } from 'rxjs';
import { errorMessage } from '../../core/api.service';
import { GlossaryApi, TermLink } from './glossary.api';
@Component({selector:'app-table-glossary',imports:[RouterLink,FormsModule],template:`
  <section class="table-glossary" aria-label="Termes métier associés">
    <header><div><h3>Termes métier associés</h3><p>Retrouvez le sens métier de cette table et de ses colonnes.</p></div><a class="wf-button" routerLink="/glossary" [queryParams]="target()">Associer un terme</a></header>
    <label>Filtrer par colonne <select [ngModel]="column()" (ngModelChange)="filter($event)"><option value="">Toute la table et ses colonnes</option>@for(c of columns();track c){<option [value]="c">{{c}}</option>}</select></label>
    @if(loading()){<p role="status">Chargement du glossaire…</p>}@else if(error()){<p role="alert">{{error()}}</p><button class="wf-button" (click)="refresh.update(v=>v+1)">Réessayer</button>}@else{
      <ul>@for(row of rows();track row.link.id){<li><a routerLink="/glossary" [queryParams]="{term:row.link.term_id}">{{row.name}}</a><span>{{row.link.column_name||'Table entière'}}</span></li>}@empty{<li>Aucun terme associé pour cette sélection.</li>}</ul>
      <div class="paging"><button class="wf-button" (click)="offset.update(v=>Math.max(0,v-20))" [disabled]="offset()===0">Précédent</button><span>Page {{offset()/20+1}}</span><button class="wf-button" (click)="offset.update(v=>v+20)" [disabled]="rows().length<20">Suivant</button></div>
    }
  </section>`,styles:`.table-glossary{padding:22px;border-top:1px solid var(--line);background:#fff}header{display:flex;justify-content:space-between;align-items:start;gap:15px;flex-wrap:wrap}p{font-size:12px;line-height:1.6;color:var(--muted);margin:8px 0 14px}label{display:grid;gap:8px;font-size:12px;max-width:320px}select{border:1px solid var(--line);padding:9px;background:white}ul{list-style:none;padding:0;margin:18px 0}li{display:flex;justify-content:space-between;gap:12px;padding:10px 0;border-bottom:1px solid var(--line);font-size:12px}li a{text-decoration:underline;overflow-wrap:anywhere}li span{color:var(--muted);overflow-wrap:anywhere}.paging{display:flex;gap:12px;align-items:center;font-size:11px}`})
export class TableGlossaryComponent {
  sourceId=input.required<string>();schema=input.required<string>();table=input.required<string>();columns=input<string[]>([]);initialColumn=input('');
  column=signal('');offset=signal(0);refresh=signal(0);loading=signal(false);error=signal('');rows=signal<{link:TermLink;name:string}[]>([]);Math=Math;
  private api=inject(GlossaryApi);
  target(){return {source_id:this.sourceId(),schema_name:this.schema(),table_name:this.table(),column_name:this.column()||null};}
  constructor(){
    effect(()=>{this.sourceId();this.schema();this.table();this.column.set(this.initialColumn());this.offset.set(0);});
    effect(onCleanup=>{const target=this.target(),offset=this.offset();this.refresh();this.loading.set(true);this.error.set('');this.rows.set([]);
      const request=this.api.lookup(target,offset).pipe(switchMap(links=>{
        const ids=[...new Set(links.map(l=>l.term_id))];return (ids.length?forkJoin(ids.map(id=>this.api.get(id))):of([])).pipe(switchMap(terms=>of(links.map(link=>({link,name:terms.find(t=>t.id===link.term_id)?.name??'Terme indisponible'})))));
      })).subscribe({next:rows=>{this.rows.set(rows);this.loading.set(false);},error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});onCleanup(()=>request.unsubscribe());
    });
  }
  filter(column:string){this.column.set(column);this.offset.set(0);}
}
