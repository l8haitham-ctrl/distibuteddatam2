import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from './auth.store';
import { safeReturnUrl } from './auth.guard';
export const authInterceptor:HttpInterceptorFn=(request,next)=>{
  const auth=inject(AuthStore),router=inject(Router);
  if(!request.url.startsWith('/api/'))return next(request);
  if(!['GET','HEAD','OPTIONS'].includes(request.method)&&auth.csrf())request=request.clone({setHeaders:{'X-CSRF-TOKEN':auth.csrf()}});
  return next(request).pipe(catchError(error=>{
    if(error.status===401&&!request.url.startsWith('/api/auth/')){auth.clear();void router.navigate(['/login'],{queryParams:{returnUrl:safeReturnUrl(router.url)}});}
    return throwError(()=>error);
  }));
};
