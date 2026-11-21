import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ExplorerStore } from '../core/explorer.store';
import { IconComponent } from '../shared/icon.component';
import { TableTreeComponent } from './table-tree.component';
import { TableStructureComponent } from './table-structure.component';
import { ConnectionDialogComponent } from './connection-dialog.component';
import { ExtractionWizardComponent } from './extraction-wizard.component';
import { Job } from '../core/pipeline.models';
import { Source } from '../../types';
@Component({selector:'app-explorer-page',imports:[IconComponent,TableTreeComponent,TableStructureComponent,ConnectionDialogComponent,ExtractionWizardComponent],templateUrl:'./explorer-page.component.html'})
export class ExplorerPageComponent {
  readonly store = inject(ExplorerStore); private router = inject(Router);
  dialog = signal(false); wizard = signal(false); defaults = signal<Source|undefined>(undefined);
  schemaCount = computed(() => new Set(this.store.tables().map(t=>t.schema_name)).size);
  newConnection(){this.defaults.set(undefined);this.dialog.set(true);}
  refresh(){if(this.store.external()){this.defaults.set(this.store.source()??undefined);this.dialog.set(true);}else this.store.loadDefault();}
  launched(job:Job){this.wizard.set(false);void this.router.navigate(['/jobs',job.id]);}
}
