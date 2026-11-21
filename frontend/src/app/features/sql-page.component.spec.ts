import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { SqlPageComponent } from './sql-page.component';

describe('SQL console interaction',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({imports:[SqlPageComponent],providers:[provideHttpClient(),provideHttpClientTesting(),{provide:ActivatedRoute,useValue:{queryParamMap:of(convertToParamMap({query:'SELECT 1'}))}}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  it('prefills without running and inserts catalog identifiers at the selection',async()=>{
    const fixture=TestBed.createComponent(SqlPageComponent);fixture.detectChanges();http.expectOne('/api/catalog/tables').flush([]);await fixture.whenStable();
    http.expectNone('/api/sql/query');expect(fixture.componentInstance.store.draft()).toBe('SELECT 1');
    const editor=fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;editor.setSelectionRange(7,8);
    fixture.componentInstance.insert('hive.runs.invoice');expect(fixture.componentInstance.store.draft()).toBe('SELECT hive.runs.invoice');
  });
  it('executes with Ctrl+Enter and renders values as text, including NULL',()=>{
    const fixture=TestBed.createComponent(SqlPageComponent);fixture.detectChanges();http.expectOne('/api/catalog/tables').flush([]);
    fixture.nativeElement.querySelector('textarea').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',ctrlKey:true,bubbles:true}));
    http.expectOne('/api/sql/query').flush({columns:[{name:'name',type:'varchar'},{name:'amount',type:'decimal'}],rows:[['<img src=x onerror=alert(1)>',null]],row_count:1,truncated:false,execution_ms:12,query_id:'test',stats:{processed_rows:1,processed_bytes:20}});
    fixture.detectChanges();expect(fixture.nativeElement.querySelector('.query-results img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.query-results').textContent).toContain('<img');expect(fixture.nativeElement.querySelector('.sql-null').textContent).toBe('NULL');
  });
});
