import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { PipelineApi } from './pipeline.api';
import { Job, activeJob } from './pipeline.models';
import { errorMessage } from './api.service';

// Scoped to the jobs route: leaving the page cancels requests and polling.
@Injectable()
export class JobsStore {
  private api = inject(PipelineApi);
  readonly jobs = signal<Job[]>([]);
  readonly selected = signal<Job|null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly detailError = signal('');
  private listRequest?: Subscription;
  private detailRequest?: Subscription;
  private timer?: ReturnType<typeof setTimeout>;
  private selectedId: string|null = null;
  private detailPending = false;
  private visible = () => {
    clearTimeout(this.timer);
    if (document.hidden) { this.listRequest?.unsubscribe(); this.detailRequest?.unsubscribe(); this.detailPending=false; this.loading.set(false); }
    else this.refresh();
  };
  constructor() {
    document.addEventListener('visibilitychange',this.visible);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.timer); this.listRequest?.unsubscribe(); this.detailRequest?.unsubscribe();
      document.removeEventListener('visibilitychange',this.visible);
    });
  }
  refresh() {
    clearTimeout(this.timer);
    this.listRequest?.unsubscribe();
    if(document.hidden) return;
    this.loading.set(true); this.error.set('');
    this.listRequest = this.api.jobs().subscribe({next: jobs => {
      this.jobs.set(jobs); this.loading.set(false);
      if(this.selectedId && !this.detailPending && (!this.selected() || activeJob(this.selected()!))) this.select(this.selectedId,false);
      this.schedule();
    },error: e => { this.error.set(errorMessage(e)); this.loading.set(false); this.schedule(); }});
  }
  select(id: string, reset = true) {
    this.selectedId = id; this.detailRequest?.unsubscribe(); this.detailError.set('');
    if(reset) this.selected.set(null);
    if(document.hidden) return;
    this.detailPending=true;
    this.detailRequest = this.api.job(id).subscribe({next: job => {
      this.detailPending=false; this.selected.set(job); this.schedule();
    },error: e => { this.detailPending=false; this.detailError.set(errorMessage(e)); }});
  }
  private schedule() {
    clearTimeout(this.timer);
    if(!document.hidden && (this.jobs().some(activeJob) || (this.selected() && activeJob(this.selected()!))))
      this.timer = setTimeout(() => this.refresh(),2000);
  }
}
