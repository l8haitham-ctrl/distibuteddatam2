import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { PipelineApi } from '../core/pipeline.api';
import { Candidate, ExtractionRequest, Job } from '../core/pipeline.models';
import { errorMessage } from '../core/api.service';
import { TableDetails } from '../../types';
import { IconComponent } from '../shared/icon.component';

@Component({selector:'app-extraction-wizard',imports:[FormsModule,JsonPipe,IconComponent],templateUrl:'./extraction-wizard.component.html'})
export class ExtractionWizardComponent implements AfterViewInit {
  table = input.required<TableDetails>(); sourceId = input.required<string>();
  closed = output<void>(); launched = output<Job>();
  dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private api = inject(PipelineApi);
  step = signal(1); candidates = signal<Candidate[]>([]); candidateLoading = signal(false);
  candidateError = signal(''); error = signal(''); busy = signal(false); uncertain = signal(false);
  format: 'parquet'|'csv' = 'parquet'; compression = 'snappy';
  granularity: 'month'|'year'|'day' = 'month'; partitions: string[] = [];
  fetchSize = 1000; numPartitions = 4;
  private lookup?: Subscription; private submission?: Subscription;
  constructor() { inject(DestroyRef).onDestroy(()=>{this.lookup?.unsubscribe();this.submission?.unsubscribe();}); }
  ngAfterViewInit() { this.dialog().nativeElement.showModal(); this.loadCandidates(); }
  changeFormat() {this.compression = this.format==='csv'?'gzip':'snappy';this.step.set(1);}
  loadCandidates() {
    this.lookup?.unsubscribe(); this.partitions=[]; this.step.set(1);
    this.candidateLoading.set(true); this.candidateError.set(''); this.candidates.set([]);
    this.lookup=this.api.candidates(this.sourceId(),this.table().schema_name,this.table().table_name,this.granularity).subscribe({
      next: c=>{this.candidates.set(c);this.candidateLoading.set(false);},
      error:e=>{this.candidateError.set(errorMessage(e));this.candidateLoading.set(false);}
    });
  }
  toggle(name:string, checked:boolean) {this.partitions=checked?[...this.partitions,name]:this.partitions.filter(n=>n!==name);}
  eligible() {return this.candidates().filter(c=>c.eligible);}
  valid() {
    return !this.candidateLoading() && !this.candidateError() && this.partitions.length<=8 &&
      this.partitions.every(n=>this.eligible().some(c=>c.name===n)) &&
      Number.isInteger(this.fetchSize) && this.fetchSize>=1 && this.fetchSize<=100000 &&
      Number.isInteger(this.numPartitions) && this.numPartitions>=1 && this.numPartitions<=16 &&
      (this.format==='parquet'?['snappy','zstd','uncompressed']:['gzip','uncompressed']).includes(this.compression);
  }
  request(): ExtractionRequest {return {source_id:this.sourceId(),schema:this.table().schema_name,table:this.table().table_name,format:this.format,compression:this.compression,partition_by:this.partitions,partition_granularity:this.granularity,fetch_size:this.fetchSize,num_partitions:this.numPartitions};}
  submit() {
    if(this.step()!==2 || !this.valid() || this.busy() || this.uncertain()) return;
    this.busy.set(true); this.error.set('');
    this.submission=this.api.create(this.request()).subscribe({next:job=>{this.busy.set(false);this.launched.emit(job);},error:e=>{
      this.busy.set(false);
      const status=(e as {status?:number}).status;
      if(status===0 || status===undefined || status>=500) {
        this.uncertain.set(true); this.error.set('La réception de la demande n’a pas pu être confirmée. Consultez les extractions avant de relancer pour éviter un doublon.');
      } else this.error.set(errorMessage(e));
    }});
  }
  cancel(event?: Event) {event?.preventDefault();if(!this.busy()) this.closed.emit();}
}
