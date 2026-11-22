import {test} from 'node:test';import assert from 'node:assert/strict';
import {summarize} from '../report.mjs';import {scenarios} from '../plan.mjs';
test('summary uses independent repetitions and rejects incomplete evidence',()=>{
 const state={reference:{row_count:100},runs:scenarios.flatMap(s=>[1,2,3].map(round=>({complete:true,round,scenario:s,server_pipeline_ms:round*100,api_wall_ms:round*110,
  result:{storage_bytes:round*10,file_count:2,extract_write_ms:round*20,verify_ms:3,registration_ms:4},
  queries:['full','january','status'].map(id=>({id,samples:[{execution_ms:round},{execution_ms:round+1},{execution_ms:round+2}]}))})))};
 const summary=summarize(state);assert.equal(summary[0].storage_bytes,20);assert.equal(summary[0].extract_write_ms,40);assert.equal(summary[0].full_sql_ms,3);
 state.runs.pop();assert.throws(()=>summarize(state),/Incomplete/);
});
