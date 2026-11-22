import {test} from 'node:test';
import assert from 'node:assert/strict';
import {scenarios,schedule,queries,validate,median} from '../plan.mjs';
test('controlled comparisons change one variable from the baseline',()=>{
 const base=scenarios.find(s=>s.id==='parquet_snappy');
 for(const id of ['parquet_month','parquet_fetch100','parquet_fetch10000','parquet_zstd','parquet_raw']){
  const target=scenarios.find(s=>s.id===id);const changed=['format','compression','fetch_size','partition_by'].filter(k=>JSON.stringify(base[k])!==JSON.stringify(target[k]));assert.equal(changed.length,1);
 }
 assert.equal(schedule().length,24);for(const s of scenarios)assert.equal(schedule().filter(x=>x.scenario.id===s.id).length,3);
 assert.notDeepEqual(schedule().slice(0,8).map(x=>x.scenario.id),schedule().slice(8,16).map(x=>x.scenario.id));
});
test('queries reject arbitrary identifiers and explicitly constrain derived partitions',()=>{
 assert.throws(()=>queries('hive.runs.x;DROP',false));assert.match(queries('hive.runs.x',true)[1].sql,/invoice_date_year=2025/);
 assert.doesNotMatch(queries('hive.runs.x',false)[1].sql,/invoice_date_year/);
});
test('integrity checks reject wrong counts, sums, groups and truncation',()=>{
 const reference={row_count:2,amount_sum:'12.30',id_sum:3,january:{row_count:1,amount_sum:'6.15'},statuses:[{status:'paid',row_count:2,amount_sum:'12.30'}]};
 validate('full',{rows:[[2,'12.30',3]]},reference);validate('status',{rows:[['paid',2,'12.30']]},reference);
 assert.throws(()=>validate('full',{rows:[[2,'12.31',3]]},reference));assert.throws(()=>validate('full',{rows:[[1,'12.30',3]]},reference));
 assert.throws(()=>validate('january',{rows:[[1,'6.14']]},reference));assert.throws(()=>validate('status',{rows:[]},reference));
 assert.throws(()=>validate('full',{rows:[[2,'12.30',3]],truncated:true},reference));
});
test('medians preserve even samples and reject missing timings',()=>{assert.equal(median([9,1,3]),3);assert.equal(median([4,2]),3);assert.throws(()=>median([]));assert.throws(()=>median([NaN]));});
