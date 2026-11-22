import {test} from 'node:test';import assert from 'node:assert/strict';
import {audit} from '../audit.mjs';
test('audit refuses partial evidence even if some timings are present',()=>{
 assert.throws(()=>audit({runs:[],warmup:{complete:true}}),/Incomplete/);
 assert.throws(()=>audit({completed_at:'now',runs:Array(24).fill({}),warmup:{complete:true}}),/Missing or duplicated/);
});
