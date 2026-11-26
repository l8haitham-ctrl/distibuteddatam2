import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ExplorerPageComponent } from './explorer-page.component';
import { NativeExplorerComponent } from './native-explorer.component';

/** One explorer route; each reader keeps its own source semantics. */
@Component({selector:'app-source-explorer',imports:[ExplorerPageComponent,NativeExplorerComponent],template:`
  @if(native()){<app-native-explorer/>}@else{<app-explorer-page/>}
`})
export class SourceExplorerComponent {
  private route=inject(ActivatedRoute);
  private params=toSignal(this.route.queryParamMap,{initialValue:this.route.snapshot.queryParamMap});
  native=computed(()=>this.params().get('source_id')?.startsWith('native:')??false);
}
