import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout } from 'rxjs';
import { Candidate, ExtractionRequest, Job, JobResult, QueryResult } from './pipeline.models';

@Injectable({providedIn:'root'})
export class PipelineApi {
  private http = inject(HttpClient);
  candidates(source: string, schema: string, table: string, granularity: string) {
    return this.http.get<Candidate[]>('/api/storage/jobs/partition-candidates', {params:{source_id:source,schema,table,partition_granularity:granularity}}).pipe(timeout(60_000));
  }
  create(request: ExtractionRequest) { return this.http.post<Job>('/api/storage/jobs',request).pipe(timeout(30_000)); }
  jobs() { return this.http.get<Job[]>('/api/storage/jobs').pipe(timeout(15_000)); }
  job(id: string) { return this.http.get<Job>(`/api/storage/jobs/${encodeURIComponent(id)}`).pipe(timeout(15_000)); }
  result(id: string) { return this.http.get<JobResult>(`/api/storage/jobs/${encodeURIComponent(id)}/result`).pipe(timeout(15_000)); }
  catalog() { return this.http.get<JobResult[]>('/api/catalog/tables').pipe(timeout(20_000)); }
  query(sql: string, max_rows: number) { return this.http.post<QueryResult>('/api/sql/query',{sql,max_rows}).pipe(timeout(75_000)); }
}
