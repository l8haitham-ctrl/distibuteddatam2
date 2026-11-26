import {readFile,writeFile} from 'node:fs/promises';
import {setTimeout as delay} from 'node:timers/promises';
import assert from 'node:assert/strict';
import {createPlatformSession} from './platform-session.mjs';

const fetchPlatform=createPlatformSession('http://backend:8000');
const path='/results/pipeline.json';
let report={source_id:'soccer',started_at:new Date().toISOString()};
try {report=JSON.parse(await readFile(path,'utf8'));} catch(error) {if(error.code!=='ENOENT')throw error;}
async function save(){await writeFile(path,JSON.stringify(report,null,2)+'\n');}
async function call(route,body){
  const response=await fetchPlatform(route,{method:body===undefined?'GET':'POST',
    headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});
  if(!response.ok)throw Error(`${route}: HTTP ${response.status}`);
  return response.json();
}
if(process.argv.includes('--preflight')){
  const jobs=await call('/storage/jobs');
  const active=jobs.filter(job=>!['COMPLETED','FAILED','CANCELLED'].includes(job.status));
  assert.equal(active.length,0,'Do not restart Spark while another extraction is active');
  console.log(JSON.stringify({existing_jobs:jobs.length,active_jobs:active.length}));
  process.exit(0);
}
assert.equal(report.source_id,'soccer');
report.sources=await call('/sources');
assert.deepEqual(report.sources.map(source=>source.id).sort(),['benchmark','postgresql','soccer','sqlserver']);
report.connection=await call('/sources/soccer/test',{});
report.tables=await call('/sources/soccer/tables');
assert.equal(report.tables.tables.length,7);
if(!report.job_id){
  if(report.submission_attempted_at)throw Error('Uncertain previous submission; inspect job history before proceeding');
  const active=(await call('/storage/jobs')).filter(job=>!['COMPLETED','FAILED','CANCELLED'].includes(job.status));
  assert.equal(active.length,0,'Another extraction is active');
  report.request={source_id:'soccer',schema:'dbo',table:'Team',format:'parquet',compression:'snappy',fetch_size:100,num_partitions:1,partition_by:[]};
  report.submission_attempted_at=new Date().toISOString();await save();
  const job=await call('/storage/jobs',report.request);report.job_id=job.id;await save();
}
console.log(`Checking extraction ${report.job_id}`);
const deadline=Date.now()+600000;
while(true){
  const job=await call('/storage/jobs/'+report.job_id);
  if(job.status==='FAILED')throw Error(`Extraction failed: ${job.error_code}`);
  if(job.status==='COMPLETED')break;
  if(Date.now()>deadline)throw Error('Timeout; keep existing job ID, do not resubmit');
  await delay(3000);
}
report.result=await call('/storage/jobs/'+report.job_id+'/result');await save();
assert.equal(report.result.row_count,299);
assert.equal(report.result.source_id,'soccer');
assert.match(report.result.catalog_table,/^[a-zA-Z0-9_.]+$/);
report.sql=await call('/sql/query',{sql:`SELECT COUNT(*) AS total FROM ${report.result.catalog_table}`,max_rows:10});
assert.equal(Number(report.sql.rows[0][0]),299);
report.completed_at=new Date().toISOString();await save();
console.log('Verified: SQL Server -> Spark -> Parquet/MinIO -> Hive -> Trino, 299 teams.');
