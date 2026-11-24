import {readFile,writeFile,rename,mkdir} from 'node:fs/promises';
import {setTimeout as delay} from 'node:timers/promises';
import {performance} from 'node:perf_hooks';
import {schedule,request,queries,validate} from './plan.mjs';
import {report} from './report.mjs';
import {audit} from './audit.mjs';
import {createPlatformSession} from '../scripts/platform-session.mjs';

const api=process.env.BENCH_API_URL??'http://backend:8000';
const platformFetch=createPlatformSession(api);
const sparkUrl=process.env.BENCH_SPARK_URL??'http://spark-master:8080/json/';
const runId=process.argv.find(a=>a.startsWith('--run-id='))?.slice(9)??new Date().toISOString().replace(/[:.]/g,'-');
if(!/^[a-zA-Z0-9_-]{1,100}$/.test(runId))throw Error('Invalid run id');
const path=`/results/${runId}.json`;
const reference=JSON.parse(await readFile('/fixtures/reference.json','utf8'));
reference.january=typeof reference.january==='string'?JSON.parse(reference.january):reference.january;
reference.statuses=typeof reference.statuses==='string'?JSON.parse(reference.statuses):reference.statuses;
await mkdir('/results',{recursive:true});
let state={version:1,run_id:runId,created_at:new Date().toISOString(),reference,plan:schedule(),sql_repetitions:3,
  cache_policy:'First query discarded as warmup; three measured repetitions. No cache flush; not cold-cache measurements.',
  phase_policy:'JDBC read + conversion + MinIO write overlap (extract_write_ms). verify_ms is output verification. registration_ms is SQL catalog loading, not isolated S3 upload.',
  system:JSON.parse(await readFile('/fixtures/system.json','utf8')),warmup:null,runs:[]};
if(process.argv.includes('--resume')){
  state=JSON.parse(await readFile(path,'utf8'));
  if(JSON.stringify(state.reference)!==JSON.stringify(reference)||JSON.stringify(state.plan)!==JSON.stringify(schedule()))throw Error('Reference or plan changed; cannot resume');
}else{try{await readFile(path);throw Error('Run already exists; use --resume');}catch(e){if(e.code!=='ENOENT')throw e;}}
async function save(){await writeFile(path+'.tmp',JSON.stringify(state,null,2)+'\n');await rename(path+'.tmp',path);}
function event(type,values={}){console.log(JSON.stringify({time:new Date().toISOString(),type,...values}));}
async function call(route,body){
  const response=await platformFetch(route,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(90000)});
  if(!response.ok)throw Error(`API ${response.status} for ${route}`);
  return response.json();
}
async function query(sql){const start=performance.now();const result=await call('/sql/query',{sql,max_rows:100});return {...result,client_ms:performance.now()-start};}
async function extract(record){
  if(!record.job_id){
    if(record.submission_attempted_at)throw Error('Uncertain API submission: inspect job history before reconciling this record; automatic resubmission is forbidden');
    record.submitted_at=new Date().toISOString();
    record.submission_attempted_at=record.submitted_at;await save();
    // Never retry this POST: a transport error may hide an accepted Spark submission.
    const job=await call('/storage/jobs',record.request);record.job_id=job.id;await save();event('submitted',{scenario:record.scenario.id,round:record.round,job_id:record.job_id});
  }
  const deadline=Date.now()+1800000;
  while(true){
    const job=await call('/storage/jobs/'+record.job_id);
    if(job.status==='FAILED')throw Error(`Job ${record.job_id} failed (${job.error_code}); inspect Spark before any new submission`);
    if(job.status==='COMPLETED'){
      const result=await call('/storage/jobs/'+record.job_id+'/result');
      if(result.row_count!==reference.row_count||result.source_id!==reference.source_id||result.table_name!==reference.table_name||result.schema_name!==reference.schema_name)throw Error('Wrong job result');
      record.finished_at=job.finished_at;record.api_wall_ms=Date.parse(job.finished_at)-Date.parse(record.submitted_at);
      record.server_pipeline_ms=Date.parse(job.finished_at)-Date.parse(job.started_at);
      record.result=result;await save();return;
    }
    if(Date.now()>deadline)throw Error(`Polling timeout; resume with existing job ${record.job_id}, do not resubmit`);
    await delay(2000);
  }
}
async function measure(record){
  record.queries??=[];
  for(const definition of queries(record.result.catalog_table,record.scenario.partition_by.length>0)){
    if(record.queries.some(q=>q.id===definition.id))continue;
    const first=await query(definition.sql);validate(definition.id,first,reference);
    const samples=[];
    for(let i=0;i<3;i++){const result=await query(definition.sql);validate(definition.id,result,reference);samples.push(result);}
    record.queries.push({...definition,warmup:first,samples});await save();
  }
  record.complete=true;await save();event('completed',{scenario:record.scenario.id,round:record.round,job_id:record.job_id,
    bytes:record.result.storage_bytes,extract_write_ms:record.result.extract_write_ms,registration_ms:record.result.registration_ms});
}
try{
  const master=await (await fetch(sparkUrl,{signal:AbortSignal.timeout(10000)})).json();
  if(master.status!=='ALIVE'||!master.workers?.some(w=>w.state==='ALIVE'))throw Error('Spark master/worker not ready');
  state.spark??={status:master.status,workers:master.workers.map(w=>({state:w.state,cores:w.cores,memory:w.memory}))};
  const active=(await call('/storage/jobs')).filter(j=>!['COMPLETED','FAILED','CANCELLED'].includes(j.status));
  const ownIds=new Set([state.warmup,...state.runs].filter(Boolean).map(r=>r.job_id));
  if(active.some(j=>!ownIds.has(j.id)))throw Error('Another ingestion is active; benchmark not started');
  await call('/sources/'+reference.source_id+'/test',{});await save();
  event('start',{run_id:runId,scenarios:8,repetitions:3,rows:reference.row_count});
  // A full extraction warms the JVM/source/storage path; excluded from all summaries.
  state.warmup??={scenario:schedule()[3].scenario,round:0,request:request(schedule()[3].scenario,reference)};
  if(!state.warmup.complete){await extract(state.warmup);validate('full',await query(queries(state.warmup.result.catalog_table,false)[0].sql),reference);state.warmup.complete=true;await save();}
  for(const entry of schedule()){
    let record=state.runs.find(r=>r.round===entry.round&&r.scenario.id===entry.scenario.id);
    if(!record){record={...entry,request:request(entry.scenario,reference)};state.runs.push(record);await save();}
    if(record.complete)continue;
    if(!record.result)await extract(record);
    await measure(record);
  }
  state.completed_at??=new Date().toISOString();state.audit=audit(state);await save();await report(state,'/results');event('finished',{run_id:runId});
}catch(error){state.last_error={time:new Date().toISOString(),message:error.message};await save();event('stopped',{message:error.message});process.exitCode=1;}
