import { Routes } from '@angular/router';
export const routes: Routes = [
  {path:'sources/native',loadComponent:()=>import('./features/native-sources.component').then(m=>m.NativeSourcesComponent)},
  {path:'sources/new',loadComponent:()=>import('./features/source-form.component').then(m=>m.SourceFormComponent)},
  {path:'sources',loadComponent:()=>import('./features/sources-page.component').then(m=>m.SourcesPageComponent)},
  {path:'explorer',loadComponent:()=>import('./features/explorer-page.component').then(m=>m.ExplorerPageComponent)},
  {path:'storage',loadComponent:()=>import('./features/storage-page.component').then(m=>m.StoragePageComponent)},
  {path:'jobs/:id',loadComponent:()=>import('./features/jobs-page.component').then(m=>m.JobsPageComponent)},
  {path:'jobs',loadComponent:()=>import('./features/jobs-page.component').then(m=>m.JobsPageComponent)},
  {path:'sql',loadComponent:()=>import('./features/sql-page.component').then(m=>m.SqlPageComponent)},
  {path:'',redirectTo:'sources',pathMatch:'full'},
  {path:'**',redirectTo:'sources'}
];
