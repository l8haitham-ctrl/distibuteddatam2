import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
export interface TermDraft {name:string;definition:string;calculation_rule:string;owner:string;status:'DRAFT'|'VALIDATED'}
export interface Target {source_id:string;schema_name:string;table_name:string;column_name?:string|null}
export interface TermLink extends Target {id:string;term_id:string;created_at:string}
export interface Term extends TermDraft {id:string;created_at:string;updated_at:string;links:TermLink[]|null}
@Injectable({providedIn:'root'})
export class GlossaryApi {
  private http=inject(HttpClient);
  list(q:string,offset=0){return this.http.get<Term[]>('/api/glossary/terms',{params:{q,offset,limit:20}}).pipe(timeout(20000));}
  get(id:string){return this.http.get<Term>(`/api/glossary/terms/${encodeURIComponent(id)}`).pipe(timeout(20000));}
  save(draft:TermDraft,id?:string){return (id?this.http.put<Term>(`/api/glossary/terms/${encodeURIComponent(id)}`,draft):this.http.post<Term>('/api/glossary/terms',draft)).pipe(timeout(20000));}
  link(id:string,target:Target){return this.http.post<TermLink>(`/api/glossary/terms/${encodeURIComponent(id)}/links`,target).pipe(timeout(30000));}
  unlink(id:string,link:string){return this.http.delete<void>(`/api/glossary/terms/${encodeURIComponent(id)}/links/${encodeURIComponent(link)}`).pipe(timeout(20000));}
  lookup(target:Target,offset=0){const params:Record<string,string|number>={source_id:target.source_id,schema_name:target.schema_name,table_name:target.table_name,limit:20,offset};if(target.column_name)params['column_name']=target.column_name;return this.http.get<TermLink[]>('/api/glossary/links',{params}).pipe(timeout(20000));}
}
