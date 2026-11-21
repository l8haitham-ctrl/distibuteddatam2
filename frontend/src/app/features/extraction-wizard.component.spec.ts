import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { ExtractionWizardComponent } from './extraction-wizard.component';
import { mockExploration } from '../../design/mockData';

describe('Extraction wizard',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({imports:[ExtractionWizardComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);Object.defineProperty(HTMLDialogElement.prototype,'showModal',{configurable:true,value:vi.fn()});});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();vi.restoreAllMocks();});
  function setup(){
    const fixture=TestBed.createComponent(ExtractionWizardComponent);fixture.componentRef.setInput('table',mockExploration.tables[1]);fixture.componentRef.setInput('sourceId','sqlserver');fixture.detectChanges();
    http.expectOne(r=>r.url.endsWith('partition-candidates')).flush([{name:'invoice_id',data_type:'int',eligible:false,reason:'Clé primaire'},{name:'invoice_date',data_type:'date',eligible:true,reason:''}]);fixture.detectChanges();return fixture;
  }
  it('offers only verified partition candidates and valid compressions',()=>{
    const fixture=setup(), c=fixture.componentInstance;
    expect(fixture.nativeElement.querySelectorAll('input[type=checkbox]')).toHaveLength(1);
    c.toggle('invoice_id',true);expect(c.valid()).toBe(false);c.partitions=[];c.format='csv';c.changeFormat();expect(c.compression).toBe('gzip');
    c.compression='snappy';expect(c.valid()).toBe(false);c.compression='uncompressed';expect(c.valid()).toBe(true);
  });
  it('reviews the JSON, submits once, and prevents automatic retry after ambiguous delivery',()=>{
    const fixture=setup(),c=fixture.componentInstance;c.step.set(2);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('pre').textContent).toContain('sqlserver');c.submit();c.submit();
    const request=http.expectOne('/api/storage/jobs');expect(request.request.body.table).toBe('Invoice');
    request.error(new ProgressEvent('error'));expect(c.uncertain()).toBe(true);c.submit();http.expectNone('/api/storage/jobs');
  });
  it('cancels stale cardinality checks when date granularity changes',()=>{
    const fixture=setup(),c=fixture.componentInstance;c.loadCandidates();const old=http.expectOne(r=>r.url.endsWith('partition-candidates'));
    c.granularity='year';c.loadCandidates();expect(old.cancelled).toBe(true);
    const next=http.expectOne(r=>r.params.get('partition_granularity')==='year');next.flush([]);expect(c.partitions).toEqual([]);expect(c.valid()).toBe(true);
  });
});
