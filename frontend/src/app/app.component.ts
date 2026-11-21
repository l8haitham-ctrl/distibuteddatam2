import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ExplorerStore } from './core/explorer.store';
import { IconComponent } from './shared/icon.component';
@Component({selector:'atlas-root',imports:[IconComponent,RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app.component.html'})
export class AppComponent { readonly store = inject(ExplorerStore); }
