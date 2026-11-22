import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { beforeEach,afterEach,describe,expect,it } from 'vitest';
import { ConnectionDialogComponent } from './connection-dialog.component';
import { mockExploration } from '../../design/mockData';

describe('Connection dialog',()=>{
  beforeEach(()=>{HTMLDialogElement.prototype.showModal=function(){this.setAttribute('open','');};TestBed.configureTestingModule({imports:[ConnectionDialogComponent],providers:[provideHttpClient(),provideHttpClientTesting()]});});
  afterEach(()=>{TestBed.inject(HttpTestingController).verify();TestBed.resetTestingModule();});
  it('handles authentication errors and clears the password on close',async()=>{
    const fixture=TestBed.createComponent(ConnectionDialogComponent);fixture.detectChanges();TestBed.inject(HttpTestingController).expectOne('/api/connectors').flush([{engine:'sqlserver',name:'Microsoft SQL Server',default_port:1433,default_schema:'dbo',available:true,capabilities:['connection']}]);await fixture.whenStable();const component=fixture.componentInstance;const password=fixture.nativeElement.querySelector('#password') as HTMLInputElement;password.value='Test-only-password!';password.dispatchEvent(new Event('input',{bubbles:true}));await fixture.whenStable();
    const button=([...fixture.nativeElement.querySelectorAll('button')] as HTMLButtonElement[]).find(b=>b.textContent.includes('Tester'))!;button.click();TestBed.inject(HttpTestingController).expectOne('/api/connections/test').flush({detail:'Identifiants refusés'},{status:400,statusText:'Bad Request'});fixture.detectChanges();expect(component.error()).toBe('Identifiants refusés');component.close();expect(component.config.password).toBe('');
  });
  it('emits metadata without credentials after connecting',async()=>{
    const fixture=TestBed.createComponent(ConnectionDialogComponent);fixture.detectChanges();TestBed.inject(HttpTestingController).expectOne('/api/connectors').flush([{engine:'sqlserver',name:'Microsoft SQL Server',default_port:1433,default_schema:'dbo',available:true,capabilities:['connection']}]);await fixture.whenStable();const component=fixture.componentInstance;const password=fixture.nativeElement.querySelector('#password') as HTMLInputElement;password.value='Test-only-password!';password.dispatchEvent(new Event('input',{bubbles:true}));await fixture.whenStable();let emitted:unknown;component.connected.subscribe(value=>emitted=value);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));await fixture.whenStable();TestBed.inject(HttpTestingController).expectOne('/api/connections/explore').flush(mockExploration);expect(JSON.stringify(emitted)).not.toContain('Test-only-password!');expect(component.config.password).toBe('');
  });
});
