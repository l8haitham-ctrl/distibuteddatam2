import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IconComponent } from '../../shared/icon.component';
import { AuthStore } from './auth.store';
import { errorMessage } from '../../core/api.service';
import { finalize } from 'rxjs';
@Component({selector:'app-platform-shell',imports:[IconComponent,RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./platform-shell.component.html'})
export class PlatformShellComponent {
  auth=inject(AuthStore);logoutBusy=signal(false);logoutError=signal('');
  logout(){if(this.logoutBusy())return;this.logoutBusy.set(true);this.logoutError.set('');this.auth.logout().pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.logoutBusy.set(false))).subscribe({next:()=>window.location.assign('/login'),error:e=>this.logoutError.set(errorMessage(e))});}
  private destroy=inject(DestroyRef);
  private router=inject(Router);
  readonly mockup=new URLSearchParams(location.search).has('maquette');
  readonly queryParams=this.mockup?{maquette:1}:{};
  section=signal('Data Sources');adding=signal(false);menu=signal(false);
  constructor(){this.router.events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd){const path=event.urlAfterRedirects.split('?')[0];this.section.set(path.startsWith('/glossary')?'Glossaire métier':path.startsWith('/explorer')?'Source Explorer':path.startsWith('/storage')?'Storage':path.startsWith('/jobs')?'Storage Jobs':path.startsWith('/sql')?'Console SQL':'Data Sources');this.adding.set(path==='/sources/new');this.menu.set(false);}});}
}
