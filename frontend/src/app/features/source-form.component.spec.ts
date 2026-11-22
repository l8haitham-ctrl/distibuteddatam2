import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { SourceFormComponent } from './source-form.component';
import { SourcesStore } from '../core/sources.store';
import { ExplorerStore } from '../core/explorer.store';
import { mockExploration } from '../../design/mockData';

describe('Full page connection form',()=>{
  let http:HttpTestingController;
  const engines=[{engine:'sqlserver',name:'Microsoft SQL Server',default_port:1433,default_schema:'dbo',available:true,capabilities:['connection','exploration','extraction']},{engine:'postgresql',name:'PostgreSQL',default_port:5432,default_schema:'public',available:true,capabilities:['connection','exploration','extraction']}];
  const explorer={apply:vi.fn()};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[SourceFormComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:ExplorerStore,useValue:explorer}]});http=TestBed.inject(HttpTestingController);vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();vi.restoreAllMocks();explorer.apply.mockClear();});
  async function setup(){const fixture=TestBed.createComponent(SourceFormComponent);http.expectOne('/api/sources').flush([]);http.expectOne('/api/connectors').flush(engines);const c=fixture.componentInstance;c.name='demo';c.config={engine:'sqlserver',host:'sqlserver',port:1433,database:'BusinessDB',username:'lake_reader',password:'private-test-only',trust_server_certificate:true};fixture.detectChanges();await fixture.whenStable();return fixture;}
  it('locks fields during a real test and never fabricates intermediate progress',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.run();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);expect(c.stageState(0)).toBe('waiting');expect(c.stageState(2)).toBe('waiting');
    http.expectOne('/api/connections/test').flush({status:'connected'});fixture.detectChanges();
    expect(c.state()).toBe('success');expect(c.stageState(3)).toBe('done');expect(c.testedAt()).not.toBeNull();
    c.changed();expect(c.state()).toBe('initial');
  });
  it('cancels pending tests and preserves entered fields after authentication errors',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.run();const request=http.expectOne('/api/connections/test');c.cancelTest();expect(request.cancelled).toBe(true);expect(c.state()).toBe('initial');
    c.run();http.expectOne('/api/connections/test').flush({detail:'Identifiants refusés par le serveur'},{status:400,statusText:'Invalid'});fixture.detectChanges();
    expect(c.config.username).toBe('lake_reader');expect(c.config.password).toBe('private-test-only');expect(c.stageState(2)).toBe('failed');expect(c.stageState(3)).toBe('skipped');
  });
  it('retains only safe session metadata and opens the original exploration workflow',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.run(true);http.expectOne('/api/connections/explore').flush(mockExploration);
    const sources=TestBed.inject(SourcesStore);expect(sources.rows()[0].temporary).toBe(true);expect(sources.rows()[0].name).toBe('demo');expect(JSON.stringify(sources.rows())).not.toContain('private-test-only');expect(c.config.password).toBe('');expect(explorer.apply).toHaveBeenCalledOnce();expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/sources']);
  });
  it('keeps unknown failure stages unconfirmed and suggests checking the server certificate',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.run();http.expectOne('/api/connections/test').flush({detail:'Connexion impossible : vérifier le serveur, le port et le certificat.'},{status:503,statusText:'Unavailable'});fixture.detectChanges();
    expect(c.stageLabel(0)).toBe('Non confirmé');expect(c.stageLabel(1)).toBe('Non confirmé');expect(c.stageNote(1)).toBe('TLS requis');expect(fixture.nativeElement.querySelector('.wf-troubleshooting').textContent).toContain('certificat du serveur');
  });
  it('uses installed PostgreSQL settings and clears the previous engine credentials and test outcome',async()=>{
    const fixture=await setup(),c=fixture.componentInstance;c.state.set('success');c.engineChanged('postgresql');fixture.detectChanges();await fixture.whenStable();
    expect(c.config.port).toBe(5432);expect(c.config.password).toBe('');expect(c.config.trust_server_certificate).toBe(false);expect(c.state()).toBe('initial');expect(fixture.nativeElement.querySelector('h1').textContent).toContain('PostgreSQL');
    c.config.password='new-test-only';fixture.detectChanges();await fixture.whenStable();c.run();const request=http.expectOne('/api/connections/test');expect(request.request.body.engine).toBe('postgresql');expect(request.request.body.port).toBe(5432);request.flush({status:'connected'});
  });
});
