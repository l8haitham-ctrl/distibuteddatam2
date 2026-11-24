import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { tap, switchMap, timeout } from 'rxjs';
export interface AuthSession {authenticated:boolean;username:string|null;csrf_token:string}
@Injectable({providedIn:'root'})
export class AuthStore {
  private http=inject(HttpClient);
  user=signal<string|null>(null);csrf=signal('');
  restore(){return this.http.get<AuthSession>('/api/auth/session').pipe(timeout(15000),tap(s=>{this.user.set(s.authenticated?s.username:null);this.csrf.set(s.csrf_token);}));}
  login(username:string,password:string){return this.http.post<void>('/api/auth/login',new HttpParams().set('username',username).set('password',password)).pipe(timeout(20000),switchMap(()=>this.restore()));}
  logout(){return this.http.post<void>('/api/auth/logout',{}).pipe(timeout(15000),tap(()=>this.clear()));}
  clear(){this.user.set(null);this.csrf.set('');}
}
