import assert from 'node:assert/strict';
import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';
import {setTimeout as delay} from 'node:timers/promises';
import {createPlatformSession} from './platform-session.mjs';

const option=(name,fallback)=>process.argv.find(value=>value.startsWith(`--${name}=`))?.slice(name.length+3)??fallback;
const output=option('output','/campaign/report.json');
const referencePath=option('references','/campaign/references.json');
const references=JSON.parse(await readFile(referencePath,'utf8'));
const platform=createPlatformSession(process.env.BENCH_API_URL??'http://backend:8000');
const formats=[['parquet','snappy'],['parquet','uncompressed'],['parquet','zstd'],['csv','uncompressed'],['csv','gzip']];
const scenarios=[];
function add(id,source,schema,table,settings={}){
  scenarios.push({id,request:{source_id:source,schema,table,format:'parquet',compression:'snappy',fetch_size:1000,num_partitions:1,partition_by:[],partition_granularity:'month',...settings}});
}
for(const [source,schema,table] of [['sqlserver','dbo','Invoice'],['postgresql','public','invoice'],['clickhouse','analytics','invoice'],['redis','redis_0','customer_segments']]){
  for(const [format,compression] of formats)add(`${source}_${format}_${compression}`,source,schema,table,{format,compression});
}
for(const granularity of ['year','month','day'])add(`sqlserver_partition_${granularity}`,'sqlserver','dbo','Invoice',{partition_by:['invoice_date'],partition_granularity:granularity});
add('sqlserver_partition_status','sqlserver','dbo','Invoice',{partition_by:['status']});
add('sqlserver_csv_gzip_month','sqlserver','dbo','Invoice',{format:'csv',compression:'gzip',partition_by:['invoice_date']});
add('sqlserver_month_and_status','sqlserver','dbo','Invoice',{partition_by:['invoice_date','status']});
add('postgresql_partition_month','postgresql','public','invoice',{partition_by:['invoice_date']});
for(const fetchSize of [100,10000])add(`sqlserver_fetch_${fetchSize}`,'sqlserver','dbo','Invoice',{fetch_size:fetchSize});
for(const readers of [2,4])add(`sqlserver_readers_${readers}`,'sqlserver','dbo','Invoice',{num_partitions:readers});
add('postgresql_readers_2','postgresql','public','invoice',{num_partitions:2});
for(const [source,schema] of [['sqlserver','dbo'],['postgresql','public'],['clickhouse','analytics']]){
  for(const table of ['Customer','Payment'])add(`${source}_${table.toLowerCase()}`,source,schema,source==='sqlserver'?table:table.toLowerCase());
}
add('benchmark_100k','benchmark','benchmark','Invoice100k',{num_partitions:2});
const soccer=Object.entries(references).find(([key])=>key.startsWith('soccer/'));
assert.ok(soccer,'Soccer source reference is required');
add('soccer_country','soccer',soccer[1].schema,soccer[1].table);
assert.equal(scenarios.length,40);
assert.equal(new Set(scenarios.map(s=>s.id)).size,scenarios.length);
if(process.argv.includes('--plan')){console.log(JSON.stringify(scenarios,null,2));process.exit(0);}

let report={campaign:'extraction-scenarios-2026-11-27',started_at:new Date().toISOString(),mode:'functional coverage; one execution per scenario, not a statistical benchmark',references,scenarios:scenarios.map(s=>({...s,state:'PENDING'})),validation_checks:[],existing_small_file_counterexamples:[{job_id:'9230be9f-620b-476c-b4e7-ad8adabf044e',files:4000},{job_id:'0a7fa971-ff80-40c7-a8bf-86406570079a',files:4770}]};
try{report=JSON.parse(await readFile(output,'utf8'));assert.deepEqual(report.scenarios.map(s=>({id:s.id,request:s.request})),scenarios,'Plan changed; inspect before resume');assert.deepEqual(report.references,references,'Source reference changed; inspect before resume');}catch(error){if(error.code!=='ENOENT')throw error;}
await mkdir(dirname(output),{recursive:true});
async function save(){await writeFile(output,JSON.stringify(report,null,2)+'\n');}
async function call(path,body){
  const response=await platform(path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(90000)});
  if(!response.ok)throw Error(`API ${response.status}: ${path}`);
  return response.json();
}
function cents(value){const match=/^(-?)(\d+)(?:\.(\d{1,2}))?$/.exec(String(value));assert.ok(match,'Expected exact monetary decimal');return (match[1]?-1n:1n)*(BigInt(match[2])*100n+BigInt((match[3]??'').padEnd(2,'0')));}
const sqlName=name=>{assert.match(name,/^[A-Za-z_][A-Za-z0-9_]*$/);return '"'+name.toLowerCase()+'"';};
async function redisReference(){const result=await call('/sql/query',{sql:"SELECT count(*),count(DISTINCT customer_id),sum(customer_id),sum(loyalty_points),count_if(segment='vip'),count_if(segment='standard') FROM redis.analytics.customer_segments",max_rows:1});return result.rows[0];}
if(!report.redis_reference){report.redis_reference=await redisReference();await save();}

for(const [index,item] of report.scenarios.entries()){
  if(item.state==='VERIFIED'||item.state==='FAILED'){continue;}
  if(!item.job_id){
    assert.ok(!item.submission_attempted_at,`Ambiguous POST for ${item.id}; inspect history before resuming`);
    const deadline=Date.now()+2100000;
    while((await call('/storage/jobs')).some(job=>!['COMPLETED','FAILED'].includes(job.status))){assert.ok(Date.now()<deadline,'Another extraction is active');await delay(3000);}
    item.state='SUBMITTING';item.submission_attempted_at=new Date().toISOString();await save();
    const job=await call('/storage/jobs',item.request);item.job_id=job.id;item.state=job.status;await save();
    console.log(JSON.stringify({event:'submitted',index:index+1,total:scenarios.length,scenario:item.id,job_id:item.job_id}));
  }
  const deadline=Date.now()+2100000;
  while(true){
    let job;
    try{job=await call('/storage/jobs/'+item.job_id);}catch(error){assert.ok(Date.now()<deadline,'Monitoring deadline; keep same job ID');console.log(JSON.stringify({event:'poll_unavailable',scenario:item.id}));await delay(5000);continue;}
    if(item.state!==job.status){item.state=job.status;await save();}
    if(job.status==='FAILED'){item.error_code=job.error_code;item.finished_at=job.finished_at;await save();console.log(JSON.stringify({event:'failed',scenario:item.id,job_id:item.job_id,error_code:item.error_code}));break;}
    if(job.status==='COMPLETED'){
      item.created_at=job.created_at;item.started_at=job.started_at;item.finished_at=job.finished_at;item.duration_ms=Date.parse(job.finished_at)-Date.parse(job.started_at);item.result=await call('/storage/jobs/'+item.job_id+'/result');
      assert.match(item.result.catalog_table,/^hive\.[a-z_0-9]+\.[a-z_0-9]+$/);
      const table=item.result.catalog_table;let sql;
      if(item.request.source_id==='redis'){
        sql=`SELECT count(*),count(DISTINCT _key),sum(CAST(customer_id AS BIGINT)),sum(CAST(loyalty_points AS BIGINT)),count_if(segment='vip'),count_if(segment='standard') FROM ${table}`;
      }else{
        const ref=report.references[item.request.source_id+'/'+item.request.table];assert.ok(ref,'Missing source reference');
        const key=sqlName(ref.key);sql=`SELECT count(*),count(DISTINCT ${key}),CAST(sum(CAST(${key} AS BIGINT)) AS VARCHAR)${ref.amount_sum!==undefined?',CAST(sum(amount) AS VARCHAR)':''} FROM ${table}`;
      }
      item.sql=await call('/sql/query',{sql,max_rows:1});assert.equal(item.sql.truncated,false);
      const values=item.sql.rows[0];
      if(item.request.source_id==='redis')assert.deepEqual(values,report.redis_reference);
      else{const ref=report.references[item.request.source_id+'/'+item.request.table];assert.equal(values[0],ref.row_count);assert.equal(values[1],ref.row_count);assert.equal(values[2],ref.id_sum);if(ref.amount_sum!==undefined)assert.equal(cents(values[3]),cents(ref.amount_sum));}
      assert.equal(item.result.row_count,values[0]);item.state='VERIFIED';item.verified_at=new Date().toISOString();await save();
      console.log(JSON.stringify({event:'verified',index:index+1,total:scenarios.length,scenario:item.id,job_id:item.job_id,rows:item.result.row_count,files:item.result.file_count,seconds:Math.round(item.duration_ms/1000)}));break;
    }
    assert.ok(Date.now()<deadline,'Monitoring deadline; keep same job ID');await delay(2000);
  }
}

// Invalid requests are not extraction jobs: record their rejections in this report,
// never insert fake failed jobs into the platform's persistent history.
const invalids=[
  ['parquet_gzip',422,{source_id:'sqlserver',schema:'dbo',table:'Invoice',format:'parquet',compression:'gzip'}],
  ['csv_snappy',422,{source_id:'sqlserver',schema:'dbo',table:'Invoice',format:'csv',compression:'snappy'}],
  ['primary_key_partition',422,{source_id:'sqlserver',schema:'dbo',table:'Invoice',partition_by:['invoice_id']}],
  ['redis_partition',422,{source_id:'redis',schema:'redis_0',table:'customer_segments',partition_by:['segment']}],
  ['minio_not_an_extraction_source',404,{source_id:'minio',table:'object'}],
  ['missing_table',404,{source_id:'sqlserver',schema:'dbo',table:'MissingScenarioTable'}],
  ['zero_readers',422,{source_id:'sqlserver',table:'Invoice',num_partitions:0}],
];
for(const [id,status,request] of invalids){
  if(report.validation_checks.some(c=>c.id===id))continue;
  const response=await platform('/storage/jobs',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(request)});
  assert.equal(response.status,status,`Unexpected validation result: ${id}`);report.validation_checks.push({id,expected_status:status,observed_status:response.status,checked_at:new Date().toISOString()});await save();
}
report.redis_reference_after=await redisReference();assert.deepEqual(report.redis_reference_after,report.redis_reference,'Redis source changed during campaign');
report.finished_at=new Date().toISOString();report.summary={total:scenarios.length,verified:report.scenarios.filter(s=>s.state==='VERIFIED').length,failed:report.scenarios.filter(s=>s.state==='FAILED').length,validation_rejections:report.validation_checks.length};await save();
console.log(JSON.stringify({event:'complete',...report.summary}));
assert.equal(report.summary.failed,0,'Some scenarios failed; inspect persisted jobs and campaign report');
