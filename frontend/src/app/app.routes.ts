import { Routes } from '@angular/router';
export const routes: Routes = [
  {path:'sources',loadComponent:()=>import('./features/explorer-page.component').then(m=>m.ExplorerPageComponent)},
  {path:'jobs/:id',loadComponent:()=>import('./features/jobs-page.component').then(m=>m.JobsPageComponent)},
  {path:'jobs',loadComponent:()=>import('./features/jobs-page.component').then(m=>m.JobsPageComponent)},
  {path:'sql',loadComponent:()=>import('./features/sql-page.component').then(m=>m.SqlPageComponent)},
  {path:'',redirectTo:'sources',pathMatch:'full'},
  {path:'**',redirectTo:'sources'}
];
