import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { afterEach,describe,expect,it,vi } from 'vitest';
import { SourcesPageComponent } from './sources-page.component';
import { ConnectorCatalog } from '../core/connector-catalog';
import { SourcesStore } from '../core/sources.store';
import { mockSource } from '../../design/mockData';

describe('Common source exploration entry',()=>{
  afterEach(()=>{TestBed.resetTestingModule();vi.restoreAllMocks();});
  it('removes the duplicate native connection link and opens every engine through the explorer route',()=>{
    TestBed.configureTestingModule({imports:[SourcesPageComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});
    vi.spyOn(TestBed.inject(ConnectorCatalog),'load').mockImplementation(()=>{});
    const fixture=TestBed.createComponent(SourcesPageComponent),http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/sources').flush([mockSource]);http.expectOne('/api/native/sources').flush([{id:'clickhouse-demo',engine:'clickhouse',host:'clickhouse-source',port:8123,namespace:'analytics',username:'reader',prefix:'',tls:false}]);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/sources/native"]')).toBeNull();
    const navigate=vi.spyOn(TestBed.inject(Router),'navigate').mockResolvedValue(true),rows=TestBed.inject(SourcesStore).rows();
    fixture.componentInstance.explore(rows[0]);expect(navigate).toHaveBeenLastCalledWith(['/explorer'],{queryParams:{source_id:mockSource.id}});
    fixture.componentInstance.explore(rows[1]);expect(navigate).toHaveBeenLastCalledWith(['/explorer'],{queryParams:{source_id:'native:clickhouse-demo'}});http.verify();
  });
});
