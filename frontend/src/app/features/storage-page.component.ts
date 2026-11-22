import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PipelineApi } from '../core/pipeline.api';
import { JobResult } from '../core/pipeline.models';
import { errorMessage } from '../core/api.service';
import { IconComponent } from '../shared/icon.component';
@Component({selector:'app-storage-page',imports:[RouterLink,DecimalPipe,IconComponent],template:`
<div class="wf-page-heading"><div><h1>Storage</h1><p>Consultez les extractions enregistrées dans MinIO et disponibles dans le catalogue SQL.</p></div><a class="wf-button wf-primary" routerLink="/sql"><app-icon name="TerminalSquare"/>Console SQL</a></div>
@if(error()){<div class="wf-alert" role="alert">{{error()}}<button class="wf-button" (click)="load()">Réessayer</button></div>}
@if(loading()){<p class="wf-empty" role="status">Chargement du stockage…</p>}@else{<section class="wf-sources-box table-scroll"><table class="wf-source-table"><thead><tr><th>Table du catalogue</th><th>Lignes</th><th>Fichiers</th><th>Octets</th><th>Emplacement</th><th>Actions</th></tr></thead><tbody>@for(table of tables();track table.job_id){<tr><th><code>{{table.catalog_table}}</code></th><td>{{table.row_count | number}}</td><td>{{table.file_count | number}}</td><td>{{table.storage_bytes | number}}</td><td><code>{{table.location}}</code></td><td><a class="wf-button" routerLink="/sql" [queryParams]="{query:table.example_query}">Interroger</a></td></tr>}@empty{<tr><td colspan="6"><div class="wf-empty"><span class="wf-empty-icon"><app-icon name="Box" [size]="22"/></span><h2>Aucune extraction enregistrée</h2><p>Terminez une extraction pour retrouver ses fichiers et sa table SQL ici.</p><a class="wf-button" routerLink="/explorer">Ouvrir Source Explorer</a></div></td></tr>}</tbody></table></section>}`})
export class StoragePageComponent {
  private api=inject(PipelineApi);private pending?:Subscription;
  tables=signal<JobResult[]>([]);loading=signal(false);error=signal('');
  constructor(){inject(DestroyRef).onDestroy(()=>this.pending?.unsubscribe());this.load();}
  load(){this.pending?.unsubscribe();this.loading.set(true);this.error.set('');this.pending=this.api.catalog().subscribe({next:rows=>{this.tables.set(rows);this.loading.set(false);},error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});}
}
