import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach,beforeEach,describe,expect,it } from 'vitest';
import { GlossaryPageComponent } from './glossary-page.component';
import { TableGlossaryComponent } from './table-glossary.component';
import { Term } from './glossary.api';
const term:Term={id:'term-1',name:'Reste à payer',definition:'<img src=x onerror=alert(1)>',calculation_rule:'Facture moins paiements',owner:'Finance',status:'DRAFT',created_at:'2026-11-24T00:00:00Z',updated_at:'2026-11-24T00:00:00Z',links:[]};
describe('Glossary workflow',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({imports:[GlossaryPageComponent,TableGlossaryComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ActivatedRoute,useValue:{queryParamMap:of(convertToParamMap({}))}}]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  function page(){const f=TestBed.createComponent(GlossaryPageComponent);f.detectChanges();http.expectOne('/api/sources').flush([{id:'sqlserver',database:'BusinessDB'}]);http.expectOne('/api/glossary/terms?q=&offset=0&limit=20').flush([term]);return f;}
  it('renders definitions as text and preserves the form after a duplicate error',()=>{
    const f=page();f.detectChanges();expect(f.nativeElement.querySelector('img')).toBeNull();expect(f.nativeElement.textContent).toContain('<img');
    f.componentInstance.create();f.componentInstance.draft={name:'Reste',definition:'Montant dû',owner:'Finance',calculation_rule:'',status:'DRAFT'};f.componentInstance.save();
    const request=http.expectOne('/api/glossary/terms');expect(request.request.method).toBe('POST');request.flush({detail:'Ce nom existe déjà.'},{status:409,statusText:'Conflict'});
    f.detectChanges();expect(f.componentInstance.editing()).toBe(true);expect(f.componentInstance.draft.definition).toBe('Montant dû');expect(f.nativeElement.querySelector('[role=alert]').textContent).toContain('existe');
  });
  it('cancels stale source requests and clears the selected table and column',()=>{
    const f=page(),c=f.componentInstance;c.sources.set([{id:'sqlserver',engine:'sqlserver',database:'BusinessDB',host:'db',port:1433,username:'reader'},{id:'postgresql',engine:'postgresql',database:'business',host:'pg',port:5432,username:'reader'}]);
    c.sourceChanged('sqlserver');const stale=http.expectOne('/api/sources/sqlserver/tables');c.sourceChanged('postgresql');expect(stale.cancelled).toBe(true);
    http.expectOne('/api/sources/postgresql/tables').flush({tables:[{schema_name:'public',table_name:'invoice'}]});c.tableChanged(c.key(c.tables()[0]));http.expectOne('/api/sources/postgresql/tables/invoice?schema=public').flush({columns:[{name:'amount'}]});
    c.column='amount';c.sourceChanged('');expect(c.selectedTable()).toBeUndefined();expect(c.column).toBe('');expect(c.columns()).toEqual([]);
  });
  it('adds and removes an association without submitting another extraction',()=>{
    const f=page(),c=f.componentInstance;c.term.set(term);c.sourceId='sqlserver';c.tables.set([{schema_name:'dbo',table_name:'Invoice'}]);c.tableKey=c.key(c.tables()[0]);c.column='amount';c.addLink();c.addLink();
    const request=http.expectOne('/api/glossary/terms/term-1/links');expect(request.request.body).toEqual({source_id:'sqlserver',schema_name:'dbo',table_name:'Invoice',column_name:'amount'});
    request.flush({id:'link-1',term_id:term.id,source_id:'sqlserver',schema_name:'dbo',table_name:'Invoice',column_name:'amount'});expect(c.term()?.links).toHaveLength(1);
    c.removeLink('link-1');const remove=http.expectOne('/api/glossary/terms/term-1/links/link-1');expect(remove.request.method).toBe('DELETE');remove.flush(null);expect(c.term()?.links).toEqual([]);http.expectNone('/api/storage/jobs');
  });
  it('searches and paginates on the server and reports source errors',()=>{
    const f=page(),c=f.componentInstance;c.q='paiement';c.search();http.expectOne('/api/glossary/terms?q=paiement&offset=0&limit=20').flush([term]);c.page(1);http.expectOne('/api/glossary/terms?q=paiement&offset=20&limit=20').flush([]);expect(c.offset).toBe(20);
    c.sourceChanged('sqlserver');http.expectOne('/api/sources/sqlserver/tables').flush({detail:'Base indisponible'},{status:503,statusText:'Unavailable'});expect(c.targetError()).toBe('Base indisponible');expect(c.targetLoading()).toBe(false);
  });
  it('looks up real associations and cancels the old table when navigation changes',()=>{
    const f=TestBed.createComponent(TableGlossaryComponent);f.componentRef.setInput('sourceId','sqlserver');f.componentRef.setInput('schema','dbo');f.componentRef.setInput('table','Invoice');f.detectChanges();
    const old=http.expectOne('/api/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice&limit=20&offset=0');
    f.componentRef.setInput('table','Payment');f.detectChanges();expect(old.cancelled).toBe(true);
    http.expectOne('/api/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Payment&limit=20&offset=0').flush([{id:'l',term_id:term.id,column_name:'amount'}]);http.expectOne('/api/glossary/terms/term-1').flush(term);f.detectChanges();expect(f.nativeElement.textContent).toContain('Reste à payer');
  });
  it('shows the linked column in the select when opened from a deep link',async()=>{
    const f=TestBed.createComponent(TableGlossaryComponent);f.componentRef.setInput('sourceId','sqlserver');f.componentRef.setInput('schema','dbo');f.componentRef.setInput('table','Invoice');f.componentRef.setInput('initialColumn','amount');f.componentRef.setInput('columns',['invoice_id','amount']);f.detectChanges();
    http.expectOne('/api/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice&limit=20&offset=0&column_name=amount').flush([]);await f.whenStable();f.detectChanges();
    expect(f.nativeElement.querySelector('select').value).toBe('amount');
    const select=f.nativeElement.querySelector('select') as HTMLSelectElement;select.value='';select.dispatchEvent(new Event('change'));f.detectChanges();http.expectOne('/api/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice&limit=20&offset=0').flush([]);expect(f.componentInstance.column()).toBe('');
  });
});
