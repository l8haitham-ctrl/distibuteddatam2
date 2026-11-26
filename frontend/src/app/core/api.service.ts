import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout } from 'rxjs';
import type { ConnectorInfo, ConnectionConfig, Exploration, Source, TableDetails, TableRef } from '../../types';
import type { NativeProfile } from './native-sources.store';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  connectors() { return this.http.get<ConnectorInfo[]>('/api/connectors').pipe(timeout(15_000)); }
  sources() { return this.http.get<Source[]>('/api/sources').pipe(timeout(15_000)); }
  nativeSources() { return this.http.get<NativeProfile[]>('/api/native/sources').pipe(timeout(15_000)); }
  testNativeSource(id:string) { return this.http.post<{status:string}>(`/api/native/sources/${encodeURIComponent(id)}/test`,{}).pipe(timeout(20_000)); }
  testSource(id: string) { return this.http.post(`/api/sources/${encodeURIComponent(id)}/test`, {}).pipe(timeout(20_000)); }
  tables(id: string) { return this.http.get<{tables: TableRef[]}>(`/api/sources/${encodeURIComponent(id)}/tables`).pipe(timeout(20_000)); }
  describe(id: string, table: TableRef) { return this.http.get<TableDetails>(`/api/sources/${encodeURIComponent(id)}/tables/${encodeURIComponent(table.table_name)}`, {params:{schema:table.schema_name}}).pipe(timeout(20_000)); }
  test(config: ConnectionConfig,timeoutMs=20_000) { return this.http.post('/api/connections/test', config).pipe(timeout(timeoutMs)); }
  explore(config: ConnectionConfig) { return this.http.post<Exploration>('/api/connections/explore', config).pipe(timeout(120_000)); }
}

export function errorMessage(error: unknown): string {
  const e = error as {name?: string; status?: number; error?: {detail?: unknown}};
  if (e.name === 'TimeoutError') return 'Délai dépassé. Vérifiez le serveur et réessayez.';
  if (typeof e.error?.detail === 'string') return e.error.detail;
  if (e.status === 0) return 'Service inaccessible. Vérifiez que les conteneurs du projet sont démarrés.';
  return 'L’opération a échoué. Vérifiez les paramètres et réessayez.';
}
