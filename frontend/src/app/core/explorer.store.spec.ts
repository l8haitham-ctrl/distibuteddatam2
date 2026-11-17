import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe,beforeEach,afterEach,expect,it } from 'vitest';
import { ExplorerStore } from './explorer.store';
import { mockExploration,mockSource } from '../../design/mockData';
import { formatType,groupTables } from '../../lib/schema';

describe('Explorer workflow',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{http.verify();TestBed.resetTestingModule();});
  function load(){const store=TestBed.inject(ExplorerStore);http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});http.expectOne('/api/sources/sqlserver/tables').flush({tables:mockExploration.tables});return store;}
  it('loads sources and cancels stale table requests',()=>{
    const store=load();const invoice=http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo');store.select(mockExploration.tables[2]);expect(invoice.cancelled).toBe(true);
    http.expectOne('/api/sources/sqlserver/tables/Payment?schema=dbo').flush(mockExploration.tables[2]);expect(store.details()?.table_name).toBe('Payment');expect(store.detailLoading()).toBe(false);
  });
  it('never replaces a real error with illustrative metadata',()=>{
    const store=TestBed.inject(ExplorerStore);http.expectOne('/api/sources').flush({detail:'Service indisponible'},{status:503,statusText:'Unavailable'});
    expect(store.error()).toBe('Service indisponible');expect(store.tables()).toHaveLength(0);expect(store.loading()).toBe(false);
  });
  it('uses temporary metadata without storing credentials',()=>{
    const store=load();http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo').flush(mockExploration.tables[1]);store.apply({...mockSource,id:'session'},mockExploration);store.select(mockExploration.tables[2]);
    expect(store.details()?.table_name).toBe('Payment');expect(store.external()).toBe(true);expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
  });
  it('supports an empty table list',()=>{
    const store=TestBed.inject(ExplorerStore);http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});http.expectOne('/api/sources/sqlserver/tables').flush({tables:[]});expect(store.selected()).toBeNull();expect(store.loading()).toBe(false);
  });
  it('preserves Unicode lengths, MAX and decimal precision',()=>{
    const column=mockExploration.tables[0].columns[1];expect(formatType(column)).toBe('nvarchar(100)');expect(formatType({...column,max_length_bytes:-1})).toBe('nvarchar(max)');expect(formatType({...column,data_type:'decimal',precision:12,scale:2})).toBe('decimal(12, 2)');expect(groupTables(mockExploration.tables,'INVOICE').get('dbo')?.[0].table_name).toBe('Invoice');
  });
});
