import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { JobsStore } from './jobs.store';
import { SqlStore } from './sql.store';
import { Job } from './pipeline.models';

const job = (status:Job['status']='RUNNING',id='one'):Job=>({id,status,request:{source_id:'sqlserver',schema:'dbo',table:'Invoice',format:'parquet',compression:'snappy',partition_by:[],partition_granularity:'month',fetch_size:1000,num_partitions:4},created_at:'2026-11-21T12:00:00Z',started_at:null,finished_at:null,error_code:null,submission_id:null,result:null});

describe('Pipeline stores',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{vi.useFakeTimers();vi.spyOn(document,'hidden','get').mockReturnValue(false);TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),JobsStore,SqlStore]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();vi.useRealTimers();vi.restoreAllMocks();});
  it('polls active jobs every two seconds and stops at completion',()=>{
    const store=TestBed.inject(JobsStore);store.refresh();http.expectOne('/api/storage/jobs').flush([job()]);
    vi.advanceTimersByTime(1999);http.expectNone('/api/storage/jobs');vi.advanceTimersByTime(1);
    http.expectOne('/api/storage/jobs').flush([job('COMPLETED')]);vi.advanceTimersByTime(6000);http.expectNone('/api/storage/jobs');
  });
  it('pauses hidden tabs and refreshes immediately on return',()=>{
    const store=TestBed.inject(JobsStore);store.refresh();http.expectOne('/api/storage/jobs').flush([job()]);
    vi.spyOn(document,'hidden','get').mockReturnValue(true);document.dispatchEvent(new Event('visibilitychange'));
    vi.advanceTimersByTime(6000);http.expectNone('/api/storage/jobs');
    vi.spyOn(document,'hidden','get').mockReturnValue(false);document.dispatchEvent(new Event('visibilitychange'));
    http.expectOne('/api/storage/jobs').flush([job('COMPLETED')]);
  });
  it('cancels a stale detail selection and retains history on refresh errors',()=>{
    const store=TestBed.inject(JobsStore);store.refresh();http.expectOne('/api/storage/jobs').flush([job('COMPLETED')]);
    store.select('one');const first=http.expectOne('/api/storage/jobs/one');store.select('two');expect(first.cancelled).toBe(true);
    http.expectOne('/api/storage/jobs/two').flush(job('COMPLETED','two'));expect(store.selected()?.id).toBe('two');
    store.refresh();http.expectOne('/api/storage/jobs').flush({detail:'Indisponible'},{status:503,statusText:'Unavailable'});
    expect(store.jobs()).toHaveLength(1);expect(store.error()).toBe('Indisponible');
  });
  it('executes SQL once, sends the row cap, and exposes truncation and typed columns',()=>{
    const store=TestBed.inject(SqlStore);store.draft.set('SELECT * FROM hive.runs.invoice');store.maxRows.set(2);store.execute();store.execute();
    const request=http.expectOne('/api/sql/query');expect(request.request.body.max_rows).toBe(2);
    request.flush({columns:[{name:'amount',type:'decimal(18,2)'}],rows:[['12.30'],[null]],row_count:2,truncated:true,execution_ms:10,query_id:'q',stats:{}});
    expect(store.result()?.truncated).toBe(true);expect(store.result()?.columns[0].type).toBe('decimal(18,2)');expect(store.loading()).toBe(false);
  });
  it('rejects invalid caps and clears stale results when SQL fails',()=>{
    const store=TestBed.inject(SqlStore);store.maxRows.set(1001);store.execute();http.expectNone('/api/sql/query');expect(store.error()).toContain('1 000');
    store.maxRows.set(100);store.execute();http.expectOne('/api/sql/query').flush({detail:'Requête refusée'},{status:422,statusText:'Invalid'});
    expect(store.error()).toBe('Requête refusée');expect(store.result()).toBeNull();expect(store.loading()).toBe(false);
  });
});
