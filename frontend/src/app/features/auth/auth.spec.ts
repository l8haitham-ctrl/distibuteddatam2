import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { afterEach,beforeEach,describe,expect,it,vi } from 'vitest';
import { AuthStore } from './auth.store';
import { authInterceptor } from './auth.interceptor';
import { authGuard, safeReturnUrl } from './auth.guard';
import { LoginPageComponent } from './login-page.component';
import { Observable } from 'rxjs';
describe('Platform authentication',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({imports:[LoginPageComponent],providers:[provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting(),provideRouter([])]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>{TestBed.resetTestingModule();http.verify();});
  it('redirects anonymous deep links before loading platform data',()=>{
    let result:unknown;const guard=TestBed.runInInjectionContext(()=>authGuard({} as ActivatedRouteSnapshot,{url:'/glossary?term=123'} as RouterStateSnapshot)) as Observable<boolean|UrlTree>;
    guard.subscribe(value=>result=value);http.expectOne('/api/auth/session').flush({authenticated:false,username:null,csrf_token:'masked'});
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fglossary%3Fterm%3D123');http.expectNone('/api/sources');
  });
  it('sends csrf to the local API, refreshes it after login, and stores no credentials',()=>{
    const auth=TestBed.inject(AuthStore);auth.restore().subscribe();http.expectOne('/api/auth/session').flush({authenticated:false,username:null,csrf_token:'before'});
    auth.login('admin','private-test-only').subscribe();const login=http.expectOne('/api/auth/login');expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('before');expect(login.request.body.get('password')).toBe('private-test-only');login.flush(null,{status:204,statusText:'No Content'});
    http.expectOne('/api/auth/session').flush({authenticated:true,username:'admin',csrf_token:'after'});expect(auth.user()).toBe('admin');
    TestBed.inject(HttpClient).post('/api/sources/sqlserver/test',{}).subscribe();const protectedCall=http.expectOne('/api/sources/sqlserver/test');expect(protectedCall.request.headers.get('X-CSRF-TOKEN')).toBe('after');protectedCall.flush({});
    TestBed.inject(HttpClient).post('https://external.example.test/action',{}).subscribe();const external=http.expectOne('https://external.example.test/action');expect(external.request.headers.has('X-CSRF-TOKEN')).toBe(false);external.flush({});expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
  });
  it('clears authentication on logout and redirects an expired session without retrying a write',()=>{
    const auth=TestBed.inject(AuthStore),router=TestBed.inject(Router);const navigate=vi.spyOn(router,'navigate').mockResolvedValue(true);auth.user.set('admin');auth.csrf.set('token');
    auth.logout().subscribe();const logout=http.expectOne('/api/auth/logout');expect(logout.request.headers.get('X-CSRF-TOKEN')).toBe('token');logout.flush(null,{status:204,statusText:'No Content'});expect(auth.user()).toBeNull();expect(auth.csrf()).toBe('');
    TestBed.inject(HttpClient).get('/api/sources').subscribe({error:()=>{}});http.expectOne('/api/sources').flush({detail:'Connexion requise.'},{status:401,statusText:'Unauthorized'});expect(navigate).toHaveBeenCalledWith(['/login'],{queryParams:{returnUrl:'/sources'}});http.expectNone('/api/auth/login');
  });
  it('keeps a failed login on the form, clears the password, and refreshes csrf',()=>{
    const fixture=TestBed.createComponent(LoginPageComponent);fixture.detectChanges();http.expectOne('/api/auth/session').flush({authenticated:false,csrf_token:'token'});fixture.componentInstance.username='admin';fixture.componentInstance.password='wrong';fixture.componentInstance.login();
    http.expectOne('/api/auth/login').flush({detail:'Identifiants incorrects.'},{status:401,statusText:'Unauthorized'});http.expectOne('/api/auth/session').flush({authenticated:false,csrf_token:'fresh'});fixture.detectChanges();expect(fixture.componentInstance.password).toBe('');expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Identifiants incorrects');expect(fixture.nativeElement.querySelector('input[type=password]')).not.toBeNull();
  });
  it('never redirects credentials to an external destination',()=>{
    for(const url of ['https://evil.test','//evil.test','/login','/\\evil.test','/api/auth/login'])expect(safeReturnUrl(url)).toBe('/sources');expect(safeReturnUrl('/jobs/123')).toBe('/jobs/123');
  });
});
