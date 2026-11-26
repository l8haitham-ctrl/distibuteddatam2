import assert from 'node:assert/strict';
import {readFile,writeFile} from 'node:fs/promises';
import {setTimeout as delay} from 'node:timers/promises';
import {createPlatformSession} from './platform-session.mjs';

const option=(name,fallback)=>process.argv.find(arg=>arg.startsWith(`--${name}=`))?.split('=')[1]??fallback;
const source=option('source','clickhouse'),table=option('table','invoice'),format=option('format','parquet');
assert.match(source,/^(clickhouse|redis)$/);assert.match(table,/^[a-z_]+$/);assert.match(format,/^(parquet|csv)$/);
const path=`/results/${source}-${table}-${format}.json`,platform=createPlatformSession('http://backend:8000');
async function call(route,body){const response=await platform(route,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(90000)});if(!response.ok)throw Error(`API ${response.status}: ${route}`);return response.json();}
async function clickhouse(sql){const response=await fetch('http://clickhouse-source:8123/',{method:'POST',headers:{'X-ClickHouse-User':'atlas_reader','X-ClickHouse-Key':process.env.CLICKHOUSE_READER_PASSWORD},body:sql+' FORMAT JSON',signal:AbortSignal.timeout(15000)});assert.equal(response.status,200);return (await response.json()).data;}
function cents(value){const [whole,fraction='']=String(value).split('.');assert.ok(fraction.length<=2);return BigInt(whole)*100n+BigInt(fraction.padEnd(2,'0'));}
let report={source,table,format,request:{source_id:source,schema:source==='redis'?'redis_0':'analytics',table,format,compression:format==='parquet'?'snappy':'gzip',fetch_size:100,partition_by:[],num_partitions:1}};
try{report=JSON.parse(await readFile(path,'utf8'));}catch(error){if(error.code!=='ENOENT')throw error;}
async function save(){await writeFile(path,JSON.stringify(report,null,2)+'\n');}
if(process.argv.includes('--active')){console.log(JSON.stringify((await call('/storage/jobs')).filter(job=>!['COMPLETED','FAILED'].includes(job.status)).map(job=>({id:job.id,status:job.status,source:job.request.source_id,table:job.request.table,created_at:job.created_at})),null,2));process.exit(0);}
const adopted=option('job-id','');
if(!report.job_id&&adopted){
  assert.equal(source,'redis','Explicit adoption is used for the Redis UI check');assert.match(adopted,/^[0-9a-f-]{36}$/);
  const job=await call('/storage/jobs/'+adopted);
  for(const key of ['source_id','schema','table','format'])assert.equal(job.request[key],report.request[key]);
  report.request=job.request;report.job_id=adopted;report.submission_attempted_at=job.created_at;report.adopted_from_ui=true;
  report.reference=await call('/sql/query',{sql:"SELECT count(*),count(DISTINCT customer_id),sum(customer_id),sum(loyalty_points),count_if(segment='vip'),count_if(segment='standard') FROM redis.analytics.customer_segments",max_rows:1});await save();
}
if(!report.job_id){
  assert.ok(!report.submission_attempted_at,'Uncertain prior submission; inspect job history');
  const active=(await call('/storage/jobs')).filter(job=>!['COMPLETED','FAILED'].includes(job.status));assert.equal(active.length,0,'Another extraction is active');
  if(source==='clickhouse')report.reference=await clickhouse(table==='invoice'?'SELECT count() AS total,toString(sum(amount)) AS amount,toString(sum(invoice_id)) AS ids,toString(min(invoice_date)) AS first_date,toString(max(invoice_date)) AS last_date FROM analytics.invoice':'SELECT count() AS total,toString(sum(customer_id)) AS ids,countIf(email IS NULL) AS null_emails,toString(min(created_at)) AS first_created FROM analytics.customer');
  if(source==='redis')report.reference=await call('/sql/query',{sql:"SELECT count(*),count(DISTINCT customer_id),sum(customer_id),sum(loyalty_points),count_if(segment='vip'),count_if(segment='standard') FROM redis.analytics.customer_segments",max_rows:1});
  report.submission_attempted_at=new Date().toISOString();await save();const job=await call('/storage/jobs',report.request);report.job_id=job.id;await save();
}
console.log(`Checking ${source}/${table}/${format} job ${report.job_id}`);
const deadline=Date.now()+1200000;
while(true){const job=await call('/storage/jobs/'+report.job_id);if(job.status==='FAILED')throw Error(`Job failed: ${job.error_code}`);if(job.status==='COMPLETED')break;assert.ok(Date.now()<deadline,'Polling expired; reuse this job ID');await delay(3000);}
report.result=await call('/storage/jobs/'+report.job_id+'/result');await save();
assert.match(report.result.catalog_table,/^hive\.[a-z_0-9]+\.[a-z_0-9]+$/);const lake=report.result.catalog_table;
let sql;
if(source==='redis')sql=`SELECT count(*),count(DISTINCT _key),sum(CAST(customer_id AS BIGINT)),sum(CAST(loyalty_points AS BIGINT)),count_if(segment='vip'),count_if(segment='standard') FROM ${lake}`;
else if(table==='invoice')sql=`SELECT count(*),CAST(sum(amount) AS VARCHAR),CAST(sum(invoice_id) AS VARCHAR),CAST(min(invoice_date) AS VARCHAR),CAST(max(invoice_date) AS VARCHAR) FROM ${lake}`;
else sql=`SELECT count(*),CAST(sum(customer_id) AS VARCHAR),count_if(email IS NULL),CAST(min(created_at) AS VARCHAR) FROM ${lake}`;
report.sql=await call('/sql/query',{sql,max_rows:10});const values=report.sql.rows[0];
if(source==='redis'){assert.deepEqual(values,[1000,1000,500500,5005000,200,800]);assert.deepEqual(values,report.reference.rows[0]);}
else {const reference=report.reference[0];assert.equal(values[0],Number(reference.total));
  if(table==='invoice'){assert.equal(cents(values[1]),cents(reference.amount));assert.equal(values[2],reference.ids);assert.equal(values[3],reference.first_date);assert.equal(values[4],reference.last_date);}
  else {assert.equal(values[1],reference.ids);assert.equal(values[2],Number(reference.null_emails));assert.ok(values[3].startsWith(reference.first_created));}
}
assert.equal(report.result.row_count,values[0]);report.completed_at=new Date().toISOString();await save();console.log(`Verified ${source}: ${values[0]} rows, source/lake SQL metrics agree.`);
