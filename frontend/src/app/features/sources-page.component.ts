import { Component, Injector, inject, signal } from '@angular/core';
import { SourceTestDialogComponent } from './connection-test/source-test-dialog.component';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SourcesStore, SourceEntry, SourceState } from '../core/sources.store';
import { ExplorerStore } from '../core/explorer.store';
import { ConnectorCatalog } from '../core/connector-catalog';
import { IconComponent } from '../shared/icon.component';

@Component({selector:'app-sources-page',imports:[FormsModule,RouterLink,IconComponent,SourceTestDialogComponent],templateUrl:'./sources-page.component.html'})
export class SourcesPageComponent {
  readonly store=inject(SourcesStore);private router=inject(Router);private injector=inject(Injector);
  readonly connectors=inject(ConnectorCatalog);
  selectedTest=signal<SourceEntry|null>(null);
  test(source:SourceEntry){this.selectedTest.set(source);this.store.test(source);}
  filters=[{id:'all',label:'Toutes'},{id:'connected',label:'Connectées'},{id:'failed',label:'En échec'},{id:'untested',label:'Non testées'}] as const;
  constructor(){this.store.load();this.connectors.load();}
  label(state:SourceState){return {connected:'Connectée',failed:'Échec',testing:'Test en cours',untested:'Non testée'}[state];}
  date(value:string|null){return value?new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}).format(new Date(value)):'Jamais';}
  engineName(source:SourceEntry){const names:Record<string,string>={redis:'Redis',clickhouse:'ClickHouse',minio:'MinIO / S3'};return source.native_profile?(names[source.engine]??source.engine):this.connectors.shortName(source.engine);}
  explore(source:SourceEntry){
    if(this.store.mockup){void this.router.navigate(['/explorer'],{queryParams:{maquette:1}});return;}
    if(source.native_profile){void this.router.navigate(['/sources/native'],{queryParams:{source_id:source.native_profile}});return;}
    if(source.temporary&&source.exploration){this.injector.get(ExplorerStore).apply(source,source.exploration);void this.router.navigate(['/explorer']);}
    else void this.router.navigate(['/explorer'],{queryParams:{source_id:source.id}});
  }
}
