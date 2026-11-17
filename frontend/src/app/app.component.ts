import { Component, computed, inject, signal } from '@angular/core';
import { ExplorerStore } from './core/explorer.store';
import { IconComponent } from './shared/icon.component';
import { TableTreeComponent } from './features/table-tree.component';
import { TableStructureComponent } from './features/table-structure.component';
import { ConnectionDialogComponent } from './features/connection-dialog.component';
import type { Source } from '../types';

@Component({selector:'atlas-root',imports:[IconComponent,TableTreeComponent,TableStructureComponent,ConnectionDialogComponent],templateUrl:'./app.component.html'})
export class AppComponent {
  readonly store = inject(ExplorerStore);
  dialog = signal(false); defaults = signal<Source|undefined>(undefined);
  schemaCount = computed(() => new Set(this.store.tables().map(t=>t.schema_name)).size);
  newConnection(){this.defaults.set(undefined);this.dialog.set(true);}
  refresh(){if(this.store.external()){this.defaults.set(this.store.source()??undefined);this.dialog.set(true);}else this.store.loadDefault();}
}
