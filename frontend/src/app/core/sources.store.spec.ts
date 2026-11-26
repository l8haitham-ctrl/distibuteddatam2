import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { Subject } from 'rxjs';
import { DiagnosticEvent, SourceTestApi } from '../features/connection-test/source-test.api';
import { SourcesStore } from './sources.store';
import { mockSource,mockExploration } from '../../design/mockData';

describe('Sources list',()=>{
  let http:HttpTestingController;
  let events:Subject<DiagnosticEvent>;let progress:ReturnType<typeof vi.fn>;
  beforeEach(()=>{events=new Subject<DiagnosticEvent>();progress=vi.fn(()=>events);TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),{provide:SourceTestApi,useValue:{progress}}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  function setup(){const store=TestBed.inject(SourcesStore);store.load();http.expectOne('/api/sources').flush([mockSource,{...mockSource,id:'archive',host:'archive-db',database:'History'}]);return store;}
  it('starts untested and filters names, hosts and databases without changing real totals',()=>{
    const store=setup();expect(store.count('untested')).toBe(2);expect(store.rows()[0].testedAt).toBeNull();
    store.search.set('ARCHIVE-db');expect(store.visible()).toHaveLength(1);expect(store.count('all')).toBe(1);expect(store.rows()).toHaveLength(2);
    store.search.set('history');expect(store.visible()[0].id).toBe('archive');store.filter.set('connected');expect(store.visible()).toHaveLength(0);
  });
  it('records actual test outcomes and prevents concurrent tests for the same source',()=>{
    const store=setup();store.test(store.rows()[0]);store.test(store.rows()[0]);expect(store.rows()[0].state).toBe('testing');
    expect(progress).toHaveBeenCalledTimes(1);events.next({step:'complete',state:'passed',message:'Connexion réussie.',duration_ms:25});events.complete();expect(store.count('connected')).toBe(1);expect(store.rows()[0].testedAt).not.toBeNull();
    events=new Subject<DiagnosticEvent>();store.test(store.rows()[0]);events.error({error:{detail:'Serveur inaccessible'},status:503});
    expect(store.rows()[0].state).toBe('failed');expect(store.rows()[0].error).toBe('Serveur inaccessible');
  });
  it('updates real stages as events arrive and retains unconfirmed state if the stream ends early',()=>{
    const store=setup();store.test(store.rows()[0]);events.next({step:'dns',state:'passed',message:'Résolu',duration_ms:2});events.next({step:'tcp',state:'running',message:'Port',duration_ms:0});expect(store.testRuns()['sqlserver'].steps.dns.state).toBe('passed');expect(store.rows()[0].state).toBe('testing');events.complete();expect(store.rows()[0].state).toBe('failed');expect(store.testRuns()['sqlserver'].steps.tcp.state).toBe('unknown');expect(store.testRuns()['sqlserver'].steps.jdbc.state).toBe('waiting');
  });
  it('never stores passwords when retaining a session and keeps it across list refreshes',()=>{
    const store=setup();store.addSession({...mockSource,id:'session',password:'secret'} as typeof mockSource,'Temporary',mockExploration);
    expect(JSON.stringify(store.rows())).not.toContain('secret');expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
    store.load(true);http.expectOne('/api/sources').flush([mockSource]);expect(store.rows().map(s=>s.id)).toEqual(['sqlserver','session']);
  });
  it('shows real API failures instead of substituting sample connections',()=>{
    const store=TestBed.inject(SourcesStore);store.load();http.expectOne('/api/sources').flush({detail:'Service indisponible'},{status:503,statusText:'Unavailable'});
    expect(store.rows()).toEqual([]);expect(store.error()).toBe('Service indisponible');
  });
});
