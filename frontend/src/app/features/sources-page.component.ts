import { Component, Injector, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SourcesStore, SourceEntry, SourceState } from '../core/sources.store';
import { ExplorerStore } from '../core/explorer.store';
import { IconComponent } from '../shared/icon.component';

@Component({selector:'app-sources-page',imports:[FormsModule,RouterLink,IconComponent],templateUrl:'./sources-page.component.html'})
export class SourcesPageComponent {
  readonly store=inject(SourcesStore);private router=inject(Router);private injector=inject(Injector);
  filters=[{id:'all',label:'Toutes'},{id:'connected',label:'Connectées'},{id:'failed',label:'En échec'},{id:'untested',label:'Non testées'}] as const;
  constructor(){this.store.load();}
  label(state:SourceState){return {connected:'Connectée',failed:'Échec',testing:'Test en cours',untested:'Non testée'}[state];}
  date(value:string|null){return value?new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}).format(new Date(value)):'Jamais';}
  explore(source:SourceEntry){
    if(this.store.mockup){void this.router.navigate(['/explorer'],{queryParams:{maquette:1}});return;}
    const explorer=this.injector.get(ExplorerStore);
    if(source.temporary&&source.exploration)explorer.apply(source,source.exploration);
    else explorer.loadDefault(source.id);
    void this.router.navigate(['/explorer']);
  }
}
