import {readFile} from 'node:fs/promises';
import {dirname} from 'node:path';
import {audit} from './audit.mjs';
import {report} from './report.mjs';
const path=process.argv[2];
if(!path)throw Error('Provide the completed campaign JSON path');
const state=JSON.parse(await readFile(path,'utf8'));
console.log(JSON.stringify(audit(state),null,2));
await report(state,dirname(path));
