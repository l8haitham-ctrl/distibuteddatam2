import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SqlStore } from '../core/sql.store';
import { IconComponent } from '../shared/icon.component';

@Component({selector:'app-sql-page',imports:[FormsModule,DecimalPipe,IconComponent],providers:[SqlStore],templateUrl:'./sql-page.component.html'})
export class SqlPageComponent {
  readonly store = inject(SqlStore);
  editor = viewChild.required<ElementRef<HTMLTextAreaElement>>('editor');
  constructor() {
    inject(ActivatedRoute).queryParamMap.pipe(takeUntilDestroyed()).subscribe(params=>{const query=params.get('query');if(query) this.store.draft.set(query.slice(0,20000));});
    this.store.loadCatalog();
  }
  key(event:KeyboardEvent) {if((event.ctrlKey || event.metaKey) && event.key==='Enter'){event.preventDefault();this.store.execute();}}
  insert(name:string) {
    const editor=this.editor().nativeElement;
    const text=this.store.draft();
    const start=editor.selectionStart, end=editor.selectionEnd;
    this.store.draft.set(text.slice(0,start)+name+text.slice(end));
    editor.value=this.store.draft(); editor.focus(); editor.setSelectionRange(start+name.length,start+name.length);
  }
  cell(value:unknown):string {return value===null?'NULL':typeof value==='object'?JSON.stringify(value):String(value);}
}
