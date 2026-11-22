import {readFile} from 'node:fs/promises';
import {pathToFileURL} from 'node:url';
import {schedule,validate,queries} from './plan.mjs';
import {summarize} from './report.mjs';

export function audit(state){
  if(!state.completed_at||state.runs.length!==24||!state.warmup?.complete)throw Error('Incomplete campaign');
  const ids=new Set(),queryIds=new Set();
  for(const entry of schedule()){
    const matches=state.runs.filter(r=>r.round===entry.round&&r.scenario.id===entry.scenario.id);
    if(matches.length!==1)throw Error('Missing or duplicated extraction');
    const r=matches[0];
    if(!r.complete||ids.has(r.job_id)||r.job_id===state.warmup.job_id)throw Error('Duplicated or incomplete job');
    ids.add(r.job_id);
    if(r.result.row_count!==state.reference.row_count||r.result.source_id!==state.reference.source_id
      ||r.result.format!==r.request.format||r.result.compression!==r.request.compression
      ||JSON.stringify(r.result.partition_by)!==JSON.stringify(r.request.partition_by)
      ||r.result.fetch_size!==r.request.fetch_size||r.result.effective_num_partitions!==2
      ||r.result.file_count<1||r.result.storage_bytes<1)throw Error('Manifest mismatch');
    for(const field of ['extract_write_ms','verify_ms','registration_ms'])if(!Number.isFinite(r.result[field])||r.result[field]<0)throw Error('Invalid phase timing');
    if(r.queries.length!==3)throw Error('Missing SQL query');
    for(const definition of queries(r.result.catalog_table,r.scenario.partition_by.length>0)){
      const matching=r.queries.filter(q=>q.id===definition.id&&q.sql===definition.sql);
      if(matching.length!==1||matching[0].samples.length!==3)throw Error('Wrong SQL protocol');
      for(const sample of [matching[0].warmup,...matching[0].samples]){
        validate(definition.id,sample,state.reference);
        if(!sample.query_id||queryIds.has(sample.query_id))throw Error('Missing or duplicated query evidence');
        queryIds.add(sample.query_id);
        if(!Number.isFinite(sample.execution_ms)||sample.execution_ms<0||sample.stats.state!=='FINISHED')throw Error('Unfinished query evidence');
      }
    }
  }
  summarize(state);
  return {extractions:ids.size,queries_with_warmups:queryIds.size,measured_queries:24*3*3,scenarios:8,integrity:'passed'};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
  console.log(JSON.stringify(audit(JSON.parse(await readFile(process.argv[2],'utf8'))),null,2));
}
