import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, finalize, of, Subject, Subscription, switchMap, tap } from 'rxjs';
import { ApiService, errorMessage } from './api.service';
import { mockExploration, mockSource } from '../../design/mockData';
import { tableKey } from '../../lib/schema';
import type { Exploration, Source, TableDetails, TableRef } from '../../types';

@Injectable({providedIn:'root'})
export class ExplorerStore {
  private api = inject(ApiService);
  private destroy = inject(DestroyRef);
  readonly mockup = new URLSearchParams(location.search).has('maquette');
  readonly source = signal<Source|null>(null);
  readonly tables = signal<TableRef[]>([]);
  readonly selected = signal<TableRef|null>(null);
  readonly details = signal<TableDetails|null>(null);
  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly error = signal('');
  readonly detailError = signal('');
  readonly external = signal(false);
  private cache: Exploration|null = null;
  private loader?: Subscription;
  private selections = new Subject<TableRef|null>();
  private requestedTarget?:TableRef;

  constructor() {
    this.selections.pipe(switchMap(table => {
      this.details.set(null); this.detailError.set('');
      if (!table || !this.source()) { this.detailLoading.set(false); return EMPTY; }
      this.detailLoading.set(true);
      const request = this.cache ? of(this.cache.tables.find(t => tableKey(t) === tableKey(table)) ?? null)
        : this.api.describe(this.source()!.id, table);
      return request.pipe(tap(result => this.details.set(result)), catchError(e => {this.detailError.set(errorMessage(e)); return EMPTY;}), finalize(() => this.detailLoading.set(false)));
    }), takeUntilDestroyed()).subscribe();
    this.destroy.onDestroy(() => this.loader?.unsubscribe());
    if (this.mockup) {
      this.source.set(mockSource); this.tables.set(mockExploration.tables); this.cache = mockExploration;
      this.select(mockExploration.tables[1]);
    } else this.loadDefault();
  }
  select(table: TableRef|null) { this.selected.set(table); this.selections.next(table); }
  retryDetails() { if(this.selected())this.select(this.selected());else this.loadDefault(this.source()?.id,this.requestedTarget); }
  loadDefault(sourceId?: string, target?:TableRef) {
    if (this.mockup) return;
    this.requestedTarget=target;
    this.loader?.unsubscribe(); this.select(null); this.loading.set(true); this.error.set('');
    this.cache = null; this.tables.set([]); this.source.set(null); this.external.set(false);
    this.loader = this.api.sources().pipe(switchMap(sources => {
      if (!sources.length) { this.error.set('Aucune source configurée. Ajoutez une connexion.'); return EMPTY; }
      const source = (sourceId ? sources.find(s=>s.id===sourceId) : sources[0]);
      if(!source){this.error.set('Source introuvable. Sélectionnez une source disponible.');return EMPTY;}
      return this.api.testSource(source.id).pipe(switchMap(() => this.api.tables(source.id)), tap(result => {
        this.source.set(source); this.tables.set(result.tables);
        if(target){const table=result.tables.find(t=>t.schema_name===target.schema_name&&t.table_name===target.table_name);if(table)this.select(table);else this.detailError.set('La table demandée est introuvable ou inaccessible.');}
        else this.select(result.tables.find(t => t.table_name === 'Invoice') ?? result.tables[0] ?? null);
      }));
    }), catchError(e => {this.error.set(errorMessage(e)); return EMPTY;}), finalize(() => this.loading.set(false))).subscribe();
  }
  apply(source: Source, result: Exploration) {
    this.loader?.unsubscribe(); this.select(null); this.cache = result;
    this.source.set(source); this.tables.set(result.tables); this.external.set(true);
    this.loading.set(false); this.error.set(''); this.select(result.tables[0] ?? null);
  }
}
