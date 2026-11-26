import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { NativeSourcesComponent } from './native-sources.component';
import { NativeSourcesStore } from '../core/native-sources.store';

describe('Native source connections',()=>{
  let http:HttpTestingController;
  const connectors=[{engine:'redis',name:'Redis',default_port:6379,capabilities:['connection','exploration']},{engine:'minio',name:'MinIO / S3',default_port:9000,capabilities:['connection','exploration']},{engine:'clickhouse',name:'ClickHouse (OLAP)',default_port:8123,capabilities:['connection','exploration']}];
  const profile={id:'redis-demo',engine:'redis',host:'redis-source',port:6379,namespace:'0',username:'atlas_reader',prefix:'analytics:',tls:false};
  const result={engine:'redis',namespace:'0',objects:[{name:'analytics:1',type:'hash',fields:['customer_id']}],truncated:false,limit:100,transport:'plain',note:'Echantillon'};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[NativeSourcesComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  async function setup(){
    const fixture=TestBed.createComponent(NativeSourcesComponent);
    http.expectOne('/api/native/connectors').flush(connectors);http.expectOne('/api/native/sources').flush([profile]);
    fixture.componentInstance.name='demo';fixture.componentInstance.config={engine:'redis',host:'redis-source',port:6379,namespace:'0',username:'reader',password:'private-test-only',tls:true,prefix:'analytics:'};
    fixture.detectChanges();await fixture.whenStable();return fixture;
  }
  it('uses configured profiles without sending credentials from the browser',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.configured(profile,true);fixture.detectChanges();
    expect(c.busy()).toBe(true);const req=http.expectOne('/api/native/sources/redis-demo/explore');expect(req.request.body).toEqual({});
    req.flush(result);fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('analytics:1');expect(c.busy()).toBe(false);
  });
  it('resets credentials and enables verified TLS on an engine change',async()=>{
    const fixture=await setup();const select=fixture.nativeElement.querySelector('select');select.value='minio';select.dispatchEvent(new Event('change'));await fixture.whenStable();
    const c=fixture.componentInstance;expect(c.config.port).toBe(9000);expect(c.config.password).toBe('');expect(c.config.tls).toBe(true);expect(c.config.namespace).toBe('');
  });
  it('keeps the explored session structure without its password',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.custom(true);
    const req=http.expectOne('/api/native/connections/explore');expect(req.request.body.namespace).toBe('0');expect(req.request.body.tls).toBe(true);
    req.flush(result);await fixture.whenStable();
    const sessions=TestBed.inject(NativeSourcesStore).sessions();expect(sessions).toHaveLength(1);expect(JSON.stringify(sessions)).not.toContain('private-test-only');expect(c.config.password).toBe('');
    expect(fixture.nativeElement.textContent).toContain('analytics:1');
  });
  it('shows server errors without announcing a successful connection',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.configured(profile,false);
    http.expectOne('/api/native/sources/redis-demo/test').flush({detail:'Connexion refusée.'},{status:502,statusText:'Bad Gateway'});
    fixture.detectChanges();expect(c.error()).toBe('Connexion refusée.');expect(c.message()).toBe('');expect(c.result()).toBeNull();
  });
  it('cancels a pending request when leaving and clears the password',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.custom(false);const req=http.expectOne('/api/native/connections/test');fixture.destroy();
    expect(req.cancelled).toBe(true);expect(c.config.password).toBe('');
  });
  it('adapts the form to a ClickHouse database and clears previous credentials',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;
    c.engineChanged('clickhouse');fixture.detectChanges();await fixture.whenStable();
    expect(c.config.port).toBe(8123);expect(c.config.tls).toBe(true);expect(c.config.password).toBe('');
    expect(fixture.nativeElement.textContent).toContain('Base de données ClickHouse');
    expect(fixture.nativeElement.textContent).toContain('Préfixe littéral des noms de tables.');
    expect(fixture.nativeElement.textContent).not.toContain('Clé d’accès');
  });
  it('renders ClickHouse table engines, typed columns and partition keys without loading rows',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;
    const ch={...profile,id:'clickhouse-demo',engine:'clickhouse',host:'clickhouse-source',port:8123,namespace:'analytics'};
    c.configured(ch,true);
    const req=http.expectOne('/api/native/sources/clickhouse-demo/explore');expect(req.request.body).toEqual({});
    req.flush({engine:'clickhouse',namespace:'analytics',objects:[{name:'invoice',type:'MergeTree',fields:['invoice_date','amount'],columns:[
      {name:'invoice_date',data_type:'Date',position:1,sorting_key:true,partition_key:true},
      {name:'amount',data_type:'Decimal(12, 2)',position:2,sorting_key:false,partition_key:false}
    ]}],truncated:false,limit:100,transport:'plain',note:'Métadonnées uniquement.'});
    fixture.detectChanges();
    const text=fixture.nativeElement.textContent;
    expect(text).toContain('MergeTree');expect(text).toContain('Decimal(12, 2)');expect(text).toContain('Partition');
    expect(fixture.nativeElement.querySelector('details summary').textContent).toContain('2 colonnes');
    expect(text).not.toContain('private-test-only');
  });
});
