import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { afterEach,describe,expect,it,vi } from 'vitest';
import { AuthStore } from '../auth/auth.store';
import { DiagnosticEvent, diagnosticFrame, readDiagnostics, SourceTestApi } from './source-test.api';
import { SourceTestDialogComponent } from './source-test-dialog.component';
describe('Connection progress transport',()=>{
  afterEach(()=>{vi.unstubAllGlobals();TestBed.resetTestingModule();});
  it('decodes split UTF-8 SSE frames without timers or simulated intermediate success',async()=>{
    const events:DiagnosticEvent[]=[],bytes=new TextEncoder().encode('event:progress\r\ndata:{"step":"dns","state":"running","message":"Résolution","duration_ms":0}\r\n\r\nevent:progress\ndata:{"step":"complete","state":"passed","message":"Réussi","duration_ms":10}\n\n');
    const stream=new ReadableStream<Uint8Array>({start(controller){for(let i=0;i<bytes.length;i+=7)controller.enqueue(bytes.slice(i,i+7));controller.close();}});
    await readDiagnostics(stream,event=>events.push(event));expect(events).toHaveLength(2);expect(events[0].message).toBe('Résolution');expect(events[1].duration_ms).toBe(10);expect(diagnosticFrame(':heartbeat')).toBeNull();expect(()=>diagnosticFrame('data:{"step":"unknown"}')).toThrow();
  });
  it('sends the authenticated csrf-protected stream request and never retries a failed request',async()=>{
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideRouter([])]});TestBed.inject(AuthStore).csrf.set('masked-token');
    const fetchMock=vi.fn().mockResolvedValue(new Response('{"detail":"Session invalide"}',{status:403,headers:{'Content-Type':'application/json'}}));vi.stubGlobal('fetch',fetchMock);
    await new Promise<void>(resolve=>TestBed.inject(SourceTestApi).progress('sqlserver').subscribe({error:()=>resolve()}));
    expect(fetchMock).toHaveBeenCalledTimes(1);const [url,options]=fetchMock.mock.calls[0];expect(url).toBe('/api/sources/sqlserver/test/events');expect(options.method).toBe('POST');expect(options.headers['X-CSRF-TOKEN']).toBe('masked-token');expect(options.credentials).toBe('same-origin');
  });
  it('renders the real stage list and keeps skipped checks distinct from failures',()=>{
    HTMLDialogElement.prototype.showModal=vi.fn();TestBed.configureTestingModule({imports:[SourceTestDialogComponent]});const fixture=TestBed.createComponent(SourceTestDialogComponent);
    fixture.componentRef.setInput('source',{id:'sqlserver',name:'sqlserver',host:'sqlserver',port:1433,database:'BusinessDB'});
    fixture.componentRef.setInput('run',{state:'failed',message:'Port inaccessible',duration_ms:15,steps:{dns:{state:'passed',message:'Résolu',duration_ms:1},tcp:{state:'failed',message:'Port inaccessible',duration_ms:14},jdbc:{state:'skipped',message:'Non exécutée',duration_ms:0},validation:{state:'skipped',message:'Non exécutée',duration_ms:0}}});fixture.detectChanges();
    const steps=[...fixture.nativeElement.querySelectorAll('li')] as HTMLElement[];expect(steps).toHaveLength(4);expect(steps[0].textContent).toContain('Réussi');expect(steps[1].textContent).toContain('Échec');expect(steps[2].textContent).toContain('Ignoré');expect(fixture.nativeElement.textContent).toContain('vérifiés ensemble');
  });
});
