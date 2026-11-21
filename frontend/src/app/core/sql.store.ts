import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { PipelineApi } from './pipeline.api';
import { JobResult, QueryResult } from './pipeline.models';
import { errorMessage } from './api.service';

@Injectable()
export class SqlStore {
  private api = inject(PipelineApi);
  draft = signal('SHOW SCHEMAS FROM hive'); maxRows = signal(1000);
  catalog = signal<JobResult[]>([]); catalogLoading = signal(false); catalogError = signal('');
  result = signal<QueryResult|null>(null); loading = signal(false); error = signal('');
  private queryRequest?: Subscription; private catalogRequest?: Subscription;
  constructor() { inject(DestroyRef).onDestroy(() => {this.queryRequest?.unsubscribe();this.catalogRequest?.unsubscribe();}); }
  loadCatalog() {
    this.catalogRequest?.unsubscribe(); this.catalogLoading.set(true); this.catalogError.set('');
    this.catalogRequest = this.api.catalog().subscribe({next: c => {this.catalog.set(c);this.catalogLoading.set(false);},error:e=>{this.catalogError.set(errorMessage(e));this.catalogLoading.set(false);}});
  }
  execute() {
    if(this.loading() || !this.draft().trim()) return;
    if(!Number.isInteger(this.maxRows()) || this.maxRows()<1 || this.maxRows()>1000) {this.error.set('La limite doit être comprise entre 1 et 1 000 lignes.');return;}
    this.error.set(''); this.result.set(null); this.loading.set(true);
    this.queryRequest = this.api.query(this.draft(),this.maxRows()).subscribe({next:r=>{this.result.set(r);this.loading.set(false);},error:e=>{this.error.set(errorMessage(e));this.loading.set(false);}});
  }
}
