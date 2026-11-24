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
  function load(){const store=TestBed.inject(ExplorerStore);store.loadSource('sqlserver');http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});http.expectOne('/api/sources/sqlserver/tables').flush({tables:mockExploration.tables});return store;}
  it('does not request sources or connect when constructed or refreshed without a choice',()=>{
    const store=TestBed.inject(ExplorerStore);store.reload();http.expectNone(()=>true);expect(store.source()).toBeNull();expect(store.tables()).toEqual([]);expect(store.loading()).toBe(false);
  });
  it('retries only the explicitly selected source after a connection failure',()=>{
    const store=TestBed.inject(ExplorerStore);store.loadSource('postgresql');http.expectOne('/api/sources').flush([mockSource,{...mockSource,id:'postgresql'}]);http.expectOne('/api/sources/postgresql/test').flush({detail:'Unavailable'},{status:503,statusText:'Unavailable'});
    store.reload();http.expectOne('/api/sources').flush([mockSource,{...mockSource,id:'postgresql'}]);http.expectOne('/api/sources/postgresql/test').flush({status:'connected'});http.expectOne('/api/sources/postgresql/tables').flush({tables:[]});http.expectNone('/api/sources/sqlserver/test');expect(store.source()?.id).toBe('postgresql');
  });
  it('never falls back to the first source for an unknown selection',()=>{
    const store=TestBed.inject(ExplorerStore);store.loadSource('removed');http.expectOne('/api/sources').flush([mockSource]);http.expectNone('/api/sources/sqlserver/test');expect(store.error()).toContain('introuvable');expect(store.source()).toBeNull();
  });
  it('loads sources and cancels stale table requests',()=>{
    const store=load();const invoice=http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo');store.select(mockExploration.tables[2]);expect(invoice.cancelled).toBe(true);
    http.expectOne('/api/sources/sqlserver/tables/Payment?schema=dbo').flush(mockExploration.tables[2]);expect(store.details()?.table_name).toBe('Payment');expect(store.detailLoading()).toBe(false);
  });
  it('never replaces a real error with illustrative metadata',()=>{
    const store=TestBed.inject(ExplorerStore);store.loadSource('sqlserver');http.expectOne('/api/sources').flush({detail:'Service indisponible'},{status:503,statusText:'Unavailable'});
    expect(store.error()).toBe('Service indisponible');expect(store.tables()).toHaveLength(0);expect(store.loading()).toBe(false);
  });
  it('uses temporary metadata without storing credentials',()=>{
    const store=load();http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo').flush(mockExploration.tables[1]);store.apply({...mockSource,id:'session'},mockExploration);store.select(mockExploration.tables[2]);
    expect(store.details()?.table_name).toBe('Payment');expect(store.external()).toBe(true);expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
  });
  it('supports an empty table list',()=>{
    const store=TestBed.inject(ExplorerStore);store.loadSource('sqlserver');http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});http.expectOne('/api/sources/sqlserver/tables').flush({tables:[]});expect(store.selected()).toBeNull();expect(store.loading()).toBe(false);
  });
  it('opens the precise source and table requested by a glossary link',()=>{
    const store=load();http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo').flush(mockExploration.tables[1]);
    store.loadSource('postgresql',{schema_name:'public',table_name:'invoice'});
    http.expectOne('/api/sources').flush([{...mockSource,id:'postgresql',engine:'postgresql'}]);http.expectOne('/api/sources/postgresql/test').flush({status:'connected'});http.expectOne('/api/sources/postgresql/tables').flush({tables:[{schema_name:'public',table_name:'invoice'}]});
    http.expectOne('/api/sources/postgresql/tables/invoice?schema=public').flush({...mockExploration.tables[1],schema_name:'public',table_name:'invoice'});expect(store.source()?.id).toBe('postgresql');expect(store.details()?.table_name).toBe('invoice');
  });
  it('reports a removed linked table instead of opening a different table',()=>{
    const store=load();http.expectOne('/api/sources/sqlserver/tables/Invoice?schema=dbo').flush(mockExploration.tables[1]);
    store.loadSource('sqlserver',{schema_name:'dbo',table_name:'Removed'});http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/sources/sqlserver/test').flush({status:'connected'});http.expectOne('/api/sources/sqlserver/tables').flush({tables:mockExploration.tables});
    expect(store.details()).toBeNull();expect(store.selected()).toBeNull();expect(store.detailError()).toContain('introuvable');
  });
  it('preserves Unicode lengths, MAX and decimal precision',()=>{
    const column=mockExploration.tables[0].columns[1];expect(formatType(column)).toBe('nvarchar(100)');expect(formatType({...column,max_length_bytes:-1})).toBe('nvarchar(max)');expect(formatType({...column,data_type:'decimal',precision:12,scale:2})).toBe('decimal(12, 2)');expect(groupTables(mockExploration.tables,'INVOICE').get('dbo')?.[0].table_name).toBe('Invoice');
  });
  it('uses logical JDBC character lengths instead of PostgreSQL byte limits',()=>{
    const column={...mockExploration.tables[0].columns[1],data_type:'varchar',max_length_bytes:400,character_length:100};expect(formatType(column)).toBe('varchar(100)');
    expect(formatType({...column,data_type:'numeric',precision:12,scale:2})).toBe('numeric(12, 2)');
  });
});
