import { Component, computed, effect, input, output, signal } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { formatType } from '../../lib/schema';
import type { TableDetails, TableRef } from '../../types';
import { TableGlossaryComponent } from './glossary/table-glossary.component';

@Component({selector:'app-table-structure',imports:[IconComponent,TableGlossaryComponent],host:{class:'table-structure',role:'region','[attr.aria-label]':'"Structure de " + table().table_name'},templateUrl:'./table-structure.component.html',styles:'.glossary-highlight{background:#eeeef0;outline:1px solid #a1a1aa;outline-offset:-1px}'})
export class TableStructureComponent {
  table = input.required<TableDetails>(); database = input.required<string>(); mockup = input(false);
  select = output<TableRef>(); tab = signal<'columns'|'relations'>('columns'); formatType = formatType;
  extractable = input(false); extract = output<void>();
  sourceId=input('');focusedColumn=input('');columnNames=computed(()=>this.table().columns.map(c=>c.name));
  primary = computed(() => new Set(this.table().primary_key.map(k => k.column_name)));
  foreign = computed(() => new Set(this.table().foreign_keys.map(k => k.column_name)));
  relations = computed(() => [...new Set(this.table().foreign_keys.map(k => k.constraint_name))]);
  constructor() { effect(() => { this.table(); this.tab.set('columns'); }); }
  relationKeys(name: string) { return this.table().foreign_keys.filter(k => k.constraint_name === name); }
  tabKey(event: KeyboardEvent) {
    if (!['ArrowLeft','ArrowRight','Home','End'].includes(event.key)) return;
    event.preventDefault();
    const next = event.key === 'Home' ? 'columns' : event.key === 'End' ? 'relations' : this.tab() === 'columns' ? 'relations' : 'columns';
    this.tab.set(next); document.getElementById('tab-' + next)?.focus();
  }
}
