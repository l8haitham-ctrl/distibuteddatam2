export const scenarios = [
  {id:'csv_raw',format:'csv',compression:'uncompressed',fetch_size:1000,partition_by:[]},
  {id:'csv_gzip',format:'csv',compression:'gzip',fetch_size:1000,partition_by:[]},
  {id:'parquet_raw',format:'parquet',compression:'uncompressed',fetch_size:1000,partition_by:[]},
  {id:'parquet_snappy',format:'parquet',compression:'snappy',fetch_size:1000,partition_by:[]},
  {id:'parquet_zstd',format:'parquet',compression:'zstd',fetch_size:1000,partition_by:[]},
  {id:'parquet_month',format:'parquet',compression:'snappy',fetch_size:1000,partition_by:['invoice_date']},
  {id:'parquet_fetch100',format:'parquet',compression:'snappy',fetch_size:100,partition_by:[]},
  {id:'parquet_fetch10000',format:'parquet',compression:'snappy',fetch_size:10000,partition_by:[]},
];
export function request(scenario,reference){return {source_id:reference.source_id,schema:reference.schema_name,table:reference.table_name,
  format:scenario.format,compression:scenario.compression,fetch_size:scenario.fetch_size,partition_by:scenario.partition_by,
  partition_granularity:'month',num_partitions:2};}
// Deterministic rotation distributes run-order effects across the three repetitions.
export function schedule(repetitions=3){return Array.from({length:repetitions},(_,round)=>scenarios.map((_,i)=>({round:round+1,scenario:scenarios[(i+round*3)%scenarios.length]}))).flat();}
export function queries(table,partitioned){
  if(!/^hive\.[a-z_][a-z0-9_]*\.[a-z_][a-z0-9_]*$/.test(table))throw Error('Invalid benchmark table');
  return [
    {id:'full',sql:`SELECT COUNT(*) AS row_count, CAST(SUM(amount) AS VARCHAR) AS amount_sum, SUM(CAST(invoice_id AS BIGINT)) AS id_sum FROM ${table}`},
    {id:'january',sql:`SELECT COUNT(*) AS row_count, CAST(SUM(amount) AS VARCHAR) AS amount_sum FROM ${table} WHERE invoice_date>=DATE '2025-01-01' AND invoice_date<DATE '2025-02-01'${partitioned?' AND invoice_date_year=2025 AND invoice_date_month=1':''}`},
    {id:'status',sql:`SELECT status, COUNT(*) AS row_count, CAST(SUM(amount) AS VARCHAR) AS amount_sum FROM ${table} GROUP BY status ORDER BY status`},
  ];
}
function decimal(value){if(!/^-?\d+\.\d{2}$/.test(String(value)))throw Error('Unexpected monetary precision');return String(value);}
export function validate(id,result,reference){
  if(result.truncated)throw Error('Truncated benchmark result');
  const rows=result.rows;
  if(id==='full'){
    if(rows.length!==1||rows[0][0]!==reference.row_count||decimal(rows[0][1])!==decimal(reference.amount_sum)||rows[0][2]!==reference.id_sum)throw Error('Full-table integrity mismatch');
  }else if(id==='january'){
    if(rows.length!==1||rows[0][0]!==reference.january.row_count||decimal(rows[0][1])!==decimal(reference.january.amount_sum))throw Error('Filtered integrity mismatch');
  }else if(id==='status'){
    const expected=reference.statuses.map(s=>[s.status,s.row_count,decimal(s.amount_sum)]);
    if(JSON.stringify(rows)!==JSON.stringify(expected))throw Error('Grouped integrity mismatch');
  }else throw Error('Unknown benchmark query');
}
export function median(values){if(!values.length||values.some(v=>!Number.isFinite(v)))throw Error('Invalid sample');const sorted=[...values].sort((a,b)=>a-b),n=sorted.length;return n%2?sorted[(n-1)/2]:(sorted[n/2-1]+sorted[n/2])/2;}
