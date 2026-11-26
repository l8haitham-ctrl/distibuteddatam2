import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../shared/icon.component';
import { groupTables, tableKey } from '../../lib/schema';
import type { TableRef } from '../../types';

@Component({selector:'app-table-tree', imports:[FormsModule,IconComponent], host:{class:'table-tree',role:'complementary','aria-label':'Structure de la base','[class.bounded-tree]':'bounded()'}, styles:':host(.bounded-tree) .schema-groups{max-height:440px;overflow:auto;overscroll-behavior:contain}', template:`
  <div class="tree-heading"><h2>Structure de la base</h2><span class="count-badge">{{tables().length}}</span></div>
  <label class="search-field"><app-icon name="Search" [size]="16"/><input [attr.aria-label]="searchLabel()" [placeholder]="searchLabel()+'…'" [ngModel]="search()" (ngModelChange)="search.set($event)"/></label>
  <div class="tree-root"><app-icon name="ChevronDown" [size]="14"/><app-icon name="Database"/><strong>{{database()}}</strong></div>
  <div class="schema-groups">
    @for(group of groups(); track group[0]) {
      <details class="schema-group" open><summary><app-icon name="ChevronDown" [size]="13"/><app-icon name="Folder" [size]="15"/><span>{{group[0]}}</span><small>{{group[1].length}}</small></summary>
        <div class="table-list">@for(table of group[1]; track key(table)) {
          <button class="table-item" [class.selected]="isSelected(table)" [attr.aria-pressed]="isSelected(table)" (click)="select.emit(table)"><app-icon [name]="itemKind()==='key'?'KeyRound':itemKind()==='file'?'Box':'Table2'" [size]="15"/><span>{{table.table_name}}</span>@if(isSelected(table)){<span class="selected-dot"></span>}</button>
        }</div>
      </details>
    } @empty {<p class="tree-empty">{{tables().length ? 'Aucun résultat pour cette recherche.' : 'Aucun élément accessible avec ce compte.'}}</p>}
  </div><div class="tree-footer"><span class="tiny-dot"></span>{{tables().length}} {{tables().length===1?singular():plural()}} disponible{{tables().length !== 1 ? 's' : ''}}</div>
`})
export class TableTreeComponent {
  tables = input.required<TableRef[]>(); selected = input<TableRef|null>(null); database = input.required<string>();
  itemKind=input<'table'|'key'|'file'>('table');
  bounded=input(false);
  singular=computed(()=>({table:'table',key:'clé',file:'fichier'})[this.itemKind()]);
  plural=computed(()=>({table:'tables',key:'clés',file:'fichiers'})[this.itemKind()]);
  searchLabel=computed(()=>({table:'Rechercher une table',key:'Rechercher une clé',file:'Rechercher un fichier'})[this.itemKind()]);
  select = output<TableRef>(); search = signal(''); key = tableKey;
  groups = computed(() => [...groupTables(this.tables(), this.search())]);
  isSelected(table: TableRef) { return this.selected() ? tableKey(table) === tableKey(this.selected()!) : false; }
}
