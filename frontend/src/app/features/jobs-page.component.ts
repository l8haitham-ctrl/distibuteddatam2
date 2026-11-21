import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, JsonPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JobsStore } from '../core/jobs.store';
import { Job, jobError, statusLabel } from '../core/pipeline.models';
import { IconComponent } from '../shared/icon.component';

@Component({selector:'app-jobs-page',imports:[DatePipe,DecimalPipe,JsonPipe,RouterLink,IconComponent],providers:[JobsStore],templateUrl:'./jobs-page.component.html'})
export class JobsPageComponent {
  readonly store = inject(JobsStore); label = statusLabel; failure = jobError;
  readonly detailMode = signal(false);
  stages = ['QUEUED','SUBMITTING','RUNNING','REGISTERING','COMPLETED'];
  stageNames = ['En attente','Soumission','Exécution Spark','Catalogue','Terminée'];
  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params=>{
      const id=params.get('id'); this.detailMode.set(!!id); if(id) this.store.select(id);
    });
    this.store.refresh();
  }
  duration(job: Job) {
    const end=job.finished_at?Date.parse(job.finished_at):Date.now();
    return Math.max(0,Math.round((end-Date.parse(job.started_at??job.created_at))/1000))+' s';
  }
  stageIndex(job:Job) {return this.stages.indexOf(job.status);}
}
