import {writeFile} from 'node:fs/promises';
import {scenarios,median} from './plan.mjs';
export function summarize(state){
  return scenarios.map(s=>{
    const runs=state.runs.filter(r=>r.complete&&r.scenario.id===s.id);
    if(runs.length!==3)throw Error(`Incomplete scenario: ${s.id}`);
    const metric=name=>median(runs.map(r=>r.result[name]));
    const sql=id=>median(runs.flatMap(r=>r.queries.find(q=>q.id===id).samples.map(q=>q.execution_ms)));
    return {scenario:s.id,format:s.format,compression:s.compression,partition:s.partition_by.length?'month':'none',fetch_size:s.fetch_size,
      repetitions:runs.length,rows:state.reference.row_count,storage_bytes:metric('storage_bytes'),file_count:metric('file_count'),
      extract_write_ms:metric('extract_write_ms'),verify_ms:metric('verify_ms'),catalog_load_ms:metric('registration_ms'),
      server_pipeline_ms:median(runs.map(r=>r.server_pipeline_ms)),api_wall_ms:median(runs.map(r=>r.api_wall_ms)),
      full_sql_ms:sql('full'),january_sql_ms:sql('january'),status_sql_ms:sql('status'),
      full_sql_min_ms:Math.min(...runs.flatMap(r=>r.queries.find(q=>q.id==='full').samples.map(q=>q.execution_ms))),
      full_sql_max_ms:Math.max(...runs.flatMap(r=>r.queries.find(q=>q.id==='full').samples.map(q=>q.execution_ms)))};
  });
}
export async function report(state,directory){
  const rows=summarize(state);const fields=Object.keys(rows[0]);
  await writeFile(`${directory}/${state.run_id}-summary.csv`,fields.join(',')+'\n'+rows.map(r=>fields.map(k=>r[k]).join(',')).join('\n')+'\n');
  await writeFile(`${directory}/${state.run_id}-summary.json`,JSON.stringify(rows,null,2)+'\n');
  const seconds=value=>(value/1000).toFixed(3);
  const lines=['# Mesures du pipeline analytique',`Execution : ${state.run_id}. ${state.reference.row_count} factures. Mediane de trois extractions ; SQL : neuf mesures par scenario apres echauffement.`,
    '', '| Scenario | MiB donnees | Fichiers | Lecture + ecriture (s) | Verification (s) | Catalogue (s) | Pipeline serveur (s) | SQL global (ms) | SQL janvier (ms) | SQL statut (ms) |',
    '| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |',
    ...rows.map(r=>`| ${r.scenario} | ${(r.storage_bytes/1048576).toFixed(3)} | ${r.file_count} | ${seconds(r.extract_write_ms)} | ${seconds(r.verify_ms)} | ${seconds(r.catalog_load_ms)} | ${seconds(r.server_pipeline_ms)} | ${r.full_sql_ms} | ${r.january_sql_ms} | ${r.status_sql_ms} |`),
    '', 'Les durees JDBC, conversion et depot MinIO se chevauchent : extract_write_ms mesure cette phase combinee. Catalogue signifie enregistrement/verification Trino, pas transfert S3 isole.',
    'Le premier passage SQL est exclu ; aucune mesure ne pretend representer un cache froid. La requete janvier ajoute des contraintes redondantes annee/mois uniquement sur la table partitionnee.',
    'Les octets sont les fichiers de donnees, hors manifeste et _SUCCESS. Les mesures brutes contiennent identifiants de jobs/requetes, SQL, repetitions, compteurs et environnement.'];
  await writeFile(`${directory}/${state.run_id}-table.md`,lines.join('\n')+'\n');
}
