import { inject } from '@angular/core';
import { CanActivateChildFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthStore } from './auth.store';
export function safeReturnUrl(url:string|null):string {
  return url&&/^\/(sources|explorer|storage|jobs|sql|glossary)(?:[/?#]|$)/.test(url)&&!/[\\\r\n]/.test(url)?url:'/sources';
}
export const authGuard:CanActivateChildFn=(_route,state)=>{
  const auth=inject(AuthStore),router=inject(Router);
  const login=()=>router.createUrlTree(['/login'],{queryParams:{returnUrl:safeReturnUrl(state.url)}});
  return auth.restore().pipe(map(s=>s.authenticated?true:login()),catchError(()=>{auth.clear();return of(login());}));
};
