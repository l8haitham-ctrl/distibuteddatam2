import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { NativeExplorerComponent } from './native-explorer.component';

describe('Unified native explorer',()=>{
  let http:HttpTestingController;let params:BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  const redis={id:'redis-demo',engine:'redis',host:'redis-source',port:6379,namespace:'0',username:'reader',prefix:'analytics:customer_segments:',tls:false,extraction_source_id:'redis',extraction_schema:'redis_0',extraction_dataset:'customer_segments'};
  const ch={...redis,id:'clickhouse-demo',engine:'clickhouse',host:'clickhouse-source',port:8123,namespace:'analytics',prefix:'',extraction_source_id:'clickhouse',extraction_schema:undefined,extraction_dataset:undefined};
  const minio={...redis,id:'minio-lake',engine:'minio',namespace:'lake',extraction_source_id:undefined,extraction_dataset:undefined};
  const redisResult={engine:'redis',namespace:'0',objects:[{name:'analytics:customer_segments:1',type:'hash',fields:['customer_id','segment']}],truncated:true,limit:100,transport:'plain',note:'Échantillon de clés.'};
  const chResult={engine:'clickhouse',namespace:'analytics',objects:[{name:'invoice',type:'MergeTree',fields:['invoice_date','amount'],columns:[{name:'invoice_date',data_type:'Date',position:1,sorting_key:true,partition_key:true},{name:'amount',data_type:'Decimal(12, 2)',position:2,sorting_key:false,partition_key:false}]},{name:'customer',type:'MergeTree',fields:[],columns:[]}],truncated:false,limit:100,transport:'plain',note:'Métadonnées uniquement.'};
  beforeEach(()=>{
    params=new BehaviorSubject(convertToParamMap({source_id:'native:redis-demo'}));
    TestBed.configureTestingModule({imports:[NativeExplorerComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{queryParamMap:params.asObservable(),snapshot:{queryParamMap:params.value}}}]});
    http=TestBed.inject(HttpTestingController);
  });
  afterEach(()=>{TestBed.resetTestingModule();http.verify();vi.restoreAllMocks();});
  async function setup(id='redis-demo'){
    params.next(convertToParamMap({source_id:'native:'+id}));const fixture=TestBed.createComponent(NativeExplorerComponent);
    http.expectOne('/api/native/connectors').flush([{engine:'redis',name:'Redis'},{engine:'clickhouse',name:'ClickHouse'},{engine:'minio',name:'MinIO / S3'}]);
    http.expectOne('/api/native/sources').flush([redis,ch,minio]);fixture.detectChanges();await fixture.whenStable();return fixture;
  }
  it('opens only the selected profile in the shared tree and detail layout without credentials',async()=>{
    const fixture=await setup();const req=http.expectOne('/api/native/sources/redis-demo/explore');expect(req.request.body).toEqual({});req.flush(redisResult);fixture.detectChanges();
    const root=fixture.nativeElement;expect(root.querySelector('h1').textContent).toBe('Source Explorer');
    expect(root.querySelector('app-table-tree')).not.toBeNull();expect(root.querySelector('.table-structure')).not.toBeNull();
    expect(root.textContent).toContain('customer_id');expect(root.textContent).toContain('Non déclaré par Redis');expect(root.textContent).toContain('Liste partielle');
    expect(root.querySelector('form')).toBeNull();expect(root.textContent).not.toContain('Ajouter une connexion personnalisée');
    http.expectNone('/api/native/sources/clickhouse-demo/explore');http.expectNone('/api/native/sources/redis-demo/test');
  });
  it('selects and filters ClickHouse tables while retaining types and sorting/partition indicators',async()=>{
    const fixture=await setup('clickhouse-demo');http.expectOne('/api/native/sources/clickhouse-demo/explore').flush(chResult);fixture.detectChanges();
    const root=fixture.nativeElement;expect(root.querySelector('.column-table').textContent).toContain('Decimal(12, 2)');
    expect(root.querySelector('.column-table').textContent).toContain('Partition');expect(root.textContent).not.toContain('clé primaire');
    const search=root.querySelector('input');search.value='customer';search.dispatchEvent(new Event('input'));await fixture.whenStable();
    expect(root.querySelectorAll('.table-item')).toHaveLength(1);root.querySelector('.table-item').click();fixture.detectChanges();
    expect(root.querySelector('.table-title h2').textContent).toBe('customer');
  });
  it('shows file metadata for MinIO and offers no source extraction',async()=>{
    const fixture=await setup('minio-lake');http.expectOne('/api/native/sources/minio-lake/explore').flush({engine:'minio',namespace:'lake',objects:[{name:'invoice/part-001.parquet',type:'parquet',fields:[],size:4096}],truncated:false,limit:100,transport:'TLS',note:'Liste de fichiers.'});fixture.detectChanges();
    const root=fixture.nativeElement;expect(root.querySelector('.column-table').textContent).toContain('4096');expect(root.querySelector('input').getAttribute('aria-label')).toBe('Rechercher un fichier');
    expect(root.querySelector('.table-heading .button.primary')).toBeNull();fixture.componentInstance.extract();http.expectNone('/api/storage/jobs');
  });
  it('submits the complete Redis dataset once, never the sampled key name',async()=>{
    const fixture=await setup();http.expectOne('/api/native/sources/redis-demo/explore').flush(redisResult);fixture.detectChanges();
    const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);fixture.componentInstance.extract();fixture.componentInstance.extract();
    const req=http.expectOne('/api/storage/jobs');expect(req.request.body).toMatchObject({source_id:'redis',schema:'redis_0',table:'customer_segments',format:'parquet',partition_by:[]});expect(JSON.stringify(req.request.body)).not.toContain('password');req.flush({id:'redis-job',status:'QUEUED'});
    expect(navigate).toHaveBeenCalledWith(['/jobs','redis-job']);
  });
  it('extracts the selected ClickHouse table through the existing job API',async()=>{
    const fixture=await setup('clickhouse-demo');http.expectOne('/api/native/sources/clickhouse-demo/explore').flush(chResult);const c=fixture.componentInstance;
    vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);c.select(chResult.objects[1]);c.extract();
    const req=http.expectOne('/api/storage/jobs');expect(req.request.body).toMatchObject({source_id:'clickhouse',schema:'analytics',table:'customer'});req.flush({id:'ch-job'});
  });
  it('blocks resubmission after an uncertain failure, including after refreshing the structure',async()=>{
    const fixture=await setup();http.expectOne('/api/native/sources/redis-demo/explore').flush(redisResult);const c=fixture.componentInstance;
    c.extract();http.expectOne('/api/storage/jobs').flush({detail:'Indisponible'},{status:503,statusText:'Unavailable'});expect(c.uncertain()).toBe(true);
    c.refresh();http.expectOne('/api/native/sources/redis-demo/explore').flush(redisResult);c.extract();http.expectNone('/api/storage/jobs');expect(c.uncertain()).toBe(true);
  });
  it('cancels stale exploration when switching native profiles',async()=>{
    const fixture=await setup();const first=http.expectOne('/api/native/sources/redis-demo/explore');
    params.next(convertToParamMap({source_id:'native:clickhouse-demo'}));fixture.detectChanges();await fixture.whenStable();expect(first.cancelled).toBe(true);
    http.expectOne('/api/native/sources/clickhouse-demo/explore').flush(chResult);fixture.detectChanges();expect(fixture.componentInstance.result()?.engine).toBe('clickhouse');
  });
  it('shows an exploration failure and retries only the same profile',async()=>{
    const fixture=await setup();http.expectOne('/api/native/sources/redis-demo/explore').flush({detail:'Accès refusé.'},{status:502,statusText:'Bad Gateway'});fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Accès refusé.');expect(fixture.nativeElement.querySelector('.table-structure')).toBeNull();
    fixture.componentInstance.refresh();http.expectOne('/api/native/sources/redis-demo/explore').flush(redisResult);expect(fixture.componentInstance.error()).toBe('');
  });
  it('handles an empty source without enabling extraction',async()=>{
    const fixture=await setup();http.expectOne('/api/native/sources/redis-demo/explore').flush({...redisResult,objects:[]});fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun élément accessible');fixture.componentInstance.extract();http.expectNone('/api/storage/jobs');
  });
  it('does not fall back to another source for an unknown profile',async()=>{
    const fixture=await setup('unknown');expect(fixture.componentInstance.error()).toContain('Source indisponible');http.expectNone(req=>req.url.endsWith('/explore'));
  });
  it('cancels a pending exploration when leaving',async()=>{
    const fixture=await setup();const req=http.expectOne('/api/native/sources/redis-demo/explore');fixture.destroy();expect(req.cancelled).toBe(true);
  });
});
