import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IconComponent } from './shared/icon.component';
@Component({selector:'atlas-root',imports:[IconComponent,RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app.component.html'})
export class AppComponent {
  private router=inject(Router);
  readonly mockup=new URLSearchParams(location.search).has('maquette');
  readonly queryParams=this.mockup?{maquette:1}:{};
  section=signal('Data Sources');adding=signal(false);menu=signal(false);
  constructor(){this.router.events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd){const path=event.urlAfterRedirects.split('?')[0];this.section.set(path.startsWith('/glossary')?'Glossaire métier':path.startsWith('/explorer')?'Source Explorer':path.startsWith('/storage')?'Storage':path.startsWith('/jobs')?'Storage Jobs':path.startsWith('/sql')?'Console SQL':'Data Sources');this.adding.set(path==='/sources/new');this.menu.set(false);}});}
}
