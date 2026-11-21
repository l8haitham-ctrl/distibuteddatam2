import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { JobsPageComponent } from './jobs-page.component';

describe('Job details',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({imports:[JobsPageComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{paramMap:of(convertToParamMap({id:'one'}))}}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  it('shows the result and SQL link without mixing the detail with the history table',()=>{
    const fixture=TestBed.createComponent(JobsPageComponent);fixture.detectChanges();
    http.expectOne('/api/storage/jobs/one').flush({id:'one',status:'COMPLETED',request:{schema:'dbo',table:'Invoice'},created_at:'2026-11-21T12:00:00Z',finished_at:'2026-11-21T12:01:00Z',result:{row_count:5000,file_count:4,storage_bytes:12000,bucket:'lake',prefix:'sqlserver/dbo/Invoice/one',catalog_table:'hive.runs.invoice_one',example_query:'SELECT * FROM hive.runs.invoice_one LIMIT 100'}});
    http.expectOne('/api/storage/jobs').flush([]);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.job-detail').textContent).toContain('s3://lake/sqlserver/dbo/Invoice/one/data/');
    expect(fixture.nativeElement.querySelector('a[href^="/sql?"]')).not.toBeNull();expect(fixture.nativeElement.querySelector('.pipeline-table')).toBeNull();
  });
  it('explains uncertain submission without offering an unsafe retry',()=>{
    const fixture=TestBed.createComponent(JobsPageComponent);fixture.detectChanges();
    http.expectOne('/api/storage/jobs/one').flush({id:'one',status:'FAILED',error_code:'SUBMISSION_UNCERTAIN',request:{schema:'dbo',table:'Invoice'},created_at:'2026-11-21T12:00:00Z',result:null});
    http.expectOne('/api/storage/jobs').flush([]);fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Soumission incertaine');expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('éviter un doublon');
    expect(fixture.nativeElement.querySelector('a[href^="/sql?"]')).toBeNull();
  });
});
