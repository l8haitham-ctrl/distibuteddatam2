import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { ConnectorCatalog } from './connector-catalog';
describe('Installed connector catalog',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  it('accepts new engines supplied by the API and excludes drivers that are not available',()=>{
    const catalog=TestBed.inject(ConnectorCatalog);catalog.load();http.expectOne('/api/connectors').flush([{engine:'custom_engine',name:'Custom JDBC',default_port:1234,default_schema:'business',available:true,capabilities:['connection']},{engine:'missing',name:'Missing driver',default_port:4321,default_schema:'public',available:false,capabilities:['connection']}]);
    expect(catalog.canConnect('custom_engine')).toBe(true);expect(catalog.get('custom_engine')?.default_port).toBe(1234);expect(catalog.canConnect('missing')).toBe(false);expect(catalog.canConnect('unknown')).toBe(false);
  });
  it('does not replace an API failure with an invented list of supported engines',()=>{
    const catalog=TestBed.inject(ConnectorCatalog);catalog.load();http.expectOne('/api/connectors').flush({detail:'Registre indisponible'},{status:503,statusText:'Unavailable'});
    expect(catalog.entries()).toEqual([]);expect(catalog.canConnect('sqlserver')).toBe(false);expect(catalog.error()).toBe('Registre indisponible');
  });
});
