import {readFile,writeFile,rename,mkdir} from 'node:fs/promises';
import {setTimeout as delay} from 'node:timers/promises';
const option=(key,fallback)=>process.argv.find(a=>a.startsWith(`--${key}=`))?.slice(key.length+3)??fallback;
const id=option('run-id',new Date().toISOString().replace(/[:.]/g,'-'));
if(!/^[a-zA-Z0-9_-]{1,100}$/.test(id))throw Error('Invalid validation id');
const file=`/reports/${id}.json`;await mkdir('/reports',{recursive:true});
let report={run_id:id,started_at:new Date().toISOString(),checks:[]};
if(process.argv.includes('--resume'))report=JSON.parse(await readFile(file,'utf8'));
else{try{await readFile(file);throw Error('Validation already exists; resume instead');}catch(e){if(e.code!=='ENOENT')throw e;}}
async function save(){await writeFile(file+'.tmp',JSON.stringify(report,null,2)+'\n');await rename(file+'.tmp',file);}
function check(name,passed,details){report.checks.push({name,status:passed?'passed':'failed',details});if(!passed)throw Error(name);}
const api=process.env.CHECK_API_URL??'http://backend:8000';
async function json(url,body){const r=await fetch(url,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(90000)});if(!r.ok)throw Error(`HTTP ${r.status} for ${new URL(url).pathname}`);return r.json();}
async function call(path,body){return json(api+path,body);}
try{
  check('readiness',(await call('/actuator/health/readiness')).status==='UP');
  check('minio_ready',(await call('/storage/status')).status==='ready');
  const frontend=process.env.CHECK_FRONTEND_URL??'http://frontend:8080';
  const html=await fetch(frontend+'/');check('angular_served',html.ok&&(await html.text()).includes('atlas-root'));
  check('nginx_api_proxy',Boolean(await json(frontend+'/api/health')));
  const sources=await call('/sources');check('sqlserver_configured',sources.some(s=>s.id==='sqlserver'));
  check('source_connection',(await call('/sources/sqlserver/test',{})).status==='connected');
  const tables=await call('/sources/sqlserver/tables');
  check('business_tables',['Customer','Invoice','Payment'].every(name=>tables.tables.some(t=>t.table_name===name)));
  const structure=await call('/sources/sqlserver/tables/Invoice?schema=dbo');
  check('invoice_structure',['invoice_id','invoice_date','amount','status'].every(name=>structure.columns.some(c=>c.name===name)));
  const master=await json(process.env.CHECK_SPARK_URL??'http://spark-master:8080/json/');
  check('spark_alive',master.status==='ALIVE'&&master.workers.some(w=>w.state==='ALIVE'));
  const existing=await call('/storage/jobs');
  if(process.argv.includes('--require-empty')&&!report.job_id)check('fresh_job_history',existing.length===0,{count:existing.length});
  const supplied=option('job-id',null);
  if(supplied&&!/^[a-f0-9-]{36}$/.test(supplied))throw Error('Invalid job id');
  if(!report.job_id&&supplied)report.job_id=supplied;
  if(!report.job_id){
    if(report.submission_attempted_at)throw Error('Uncertain submission; inspect the history before reconciling this validation');
    check('no_other_active_jobs',existing.every(j=>['COMPLETED','FAILED'].includes(j.status)));
    report.submission_attempted_at=new Date().toISOString();await save();
    report.request={source_id:'sqlserver',table:'Invoice',format:'parquet',compression:'snappy',partition_by:['invoice_date'],fetch_size:1000,num_partitions:2};
    const job=await call('/storage/jobs',report.request);report.job_id=job.id;await save();
  }
  const deadline=Date.now()+900000;
  while(true){
    const job=await call(`/storage/jobs/${report.job_id}`);
    if(job.status==='FAILED')throw Error(`Job failed (${job.error_code}); no automatic resubmission`);
    if(job.status==='COMPLETED'){report.job=job;break;}
    if(Date.now()>deadline)throw Error('Known job still pending; resume without submitting a second job');
    await delay(2000);
  }
  const result=await call(`/storage/jobs/${report.job_id}/result`);report.result=result;
  check('source_job_identity',result.source_id==='sqlserver'&&result.table_name==='Invoice'&&result.schema_name==='dbo');
  check('stored_rows',result.row_count===5000,{rows:result.row_count});
  check('stored_files',result.file_count>0&&result.storage_bytes>0,{files:result.file_count,bytes:result.storage_bytes});
  check('catalog_registered',(await call('/catalog/tables')).some(t=>t.job_id===report.job_id&&t.catalog_table===result.catalog_table));
  if(!/^hive\.[a-z_][a-z0-9_]*\.[a-z_][a-z0-9_]*$/.test(result.catalog_table))throw Error('Invalid catalog table');
  const sql=`SELECT COUNT(*) AS invoices, CAST(SUM(amount) AS VARCHAR) AS amount_sum FROM ${result.catalog_table}`;
  report.sql=await call('/sql/query',{sql,max_rows:10});report.sql_text=sql;
  check('sql_reconciliation',!report.sql.truncated&&report.sql.rows[0][0]===5000&&report.sql.rows[0][1]==='4892425.00',{rows:report.sql.rows});
  const native=await call('/native/sources');
  check('native_minio_connected',native.some(n=>n.id==='minio-lake')&&(await call('/native/sources/minio-lake/test',{})).status==='connected');
  report.status='passed';report.completed_at=new Date().toISOString();await save();
  console.log(JSON.stringify({status:report.status,run_id:id,job_id:report.job_id,checks:report.checks.length}));
}catch(error){report.status='failed';report.error=error.message;await save();console.error(error.message);process.exitCode=1;}
