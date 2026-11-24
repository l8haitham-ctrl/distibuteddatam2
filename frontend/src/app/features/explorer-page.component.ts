import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ExplorerStore } from '../core/explorer.store';
import { IconComponent } from '../shared/icon.component';
import { TableTreeComponent } from './table-tree.component';
import { TableStructureComponent } from './table-structure.component';
import { ConnectionDialogComponent } from './connection-dialog.component';
import { ExtractionWizardComponent } from './extraction-wizard.component';
import { Job } from '../core/pipeline.models';
import { Source, TableRef } from '../../types';
import { ConnectorCatalog } from '../core/connector-catalog';
@Component({selector:'app-explorer-page',imports:[RouterLink,IconComponent,TableTreeComponent,TableStructureComponent,ConnectionDialogComponent,ExtractionWizardComponent],templateUrl:'./explorer-page.component.html'})
export class ExplorerPageComponent {
  readonly store = inject(ExplorerStore); private router = inject(Router);
  readonly connectors=inject(ConnectorCatalog);
  focusedColumn=signal('');
  constructor(){this.connectors.load();inject(ActivatedRoute).queryParamMap.pipe(takeUntilDestroyed()).subscribe(p=>{this.focusedColumn.set(p.get('column_name')??'');const source=p.get('source_id'),schema=p.get('schema_name'),table=p.get('table_name');if(source)this.store.loadSource(source,schema&&table?{schema_name:schema,table_name:table}:undefined);});}
  dialog = signal(false); wizard = signal(false); defaults = signal<Source|undefined>(undefined);
  schemaCount = computed(() => new Set(this.store.tables().map(t=>t.schema_name)).size);
  newConnection(){void this.router.navigate(['/sources/new'],{queryParams:this.store.mockup?{maquette:1}:{}});}
  refresh(){if(this.store.external()){this.defaults.set(this.store.source()??undefined);this.dialog.set(true);}else this.store.reload();}
  selectTable(table:TableRef){this.focusedColumn.set('');this.store.select(table);}
  launched(job:Job){this.wizard.set(false);void this.router.navigate(['/jobs',job.id]);}
}
