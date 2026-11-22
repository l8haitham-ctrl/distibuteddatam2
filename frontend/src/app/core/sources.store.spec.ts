import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { SourcesStore } from './sources.store';
import { mockSource,mockExploration } from '../../design/mockData';

describe('Sources list',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  function setup(){const store=TestBed.inject(SourcesStore);store.load();http.expectOne('/api/sources').flush([mockSource,{...mockSource,id:'archive',host:'archive-db',database:'History'}]);return store;}
  it('starts untested and filters names, hosts and databases without changing real totals',()=>{
    const store=setup();expect(store.count('untested')).toBe(2);expect(store.rows()[0].testedAt).toBeNull();
    store.search.set('ARCHIVE-db');expect(store.visible()).toHaveLength(1);expect(store.count('all')).toBe(1);expect(store.rows()).toHaveLength(2);
    store.search.set('history');expect(store.visible()[0].id).toBe('archive');store.filter.set('connected');expect(store.visible()).toHaveLength(0);
  });
  it('records actual test outcomes and prevents concurrent tests for the same source',()=>{
    const store=setup();store.test(store.rows()[0]);store.test(store.rows()[0]);expect(store.rows()[0].state).toBe('testing');
    http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});expect(store.count('connected')).toBe(1);expect(store.rows()[0].testedAt).not.toBeNull();
    store.test(store.rows()[0]);http.expectOne('/api/sources/sqlserver/test').flush({detail:'Serveur inaccessible'},{status:503,statusText:'Unavailable'});
    expect(store.rows()[0].state).toBe('failed');expect(store.rows()[0].error).toBe('Serveur inaccessible');
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
