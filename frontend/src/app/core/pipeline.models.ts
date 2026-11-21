export interface ExtractionRequest {
  source_id: string; schema: string; table: string; format: 'parquet'|'csv';
  compression: string; partition_by: string[]; partition_granularity: 'month'|'year'|'day';
  fetch_size: number; num_partitions: number;
}
export interface Candidate { name: string; data_type: string; eligible: boolean; reason: string; }
export type JobStatus = 'QUEUED'|'SUBMITTING'|'RUNNING'|'REGISTERING'|'COMPLETED'|'FAILED';
export interface Job {
  id: string; status: JobStatus; request: ExtractionRequest; created_at: string;
  started_at: string|null; finished_at: string|null; error_code: string|null;
  submission_id: string|null; result: JobResult|null;
}
export interface JobResult {
  job_id?: string; row_count?: number; file_count?: number; storage_bytes?: number;
  bucket?: string; prefix?: string; location?: string; manifest_key?: string;
  catalog_table?: string; current_view?: string; example_query?: string;
  extract_write_ms?: number; verify_ms?: number; spark_duration_ms?: number; registration_ms?: number;
}
export interface QueryResult {
  columns: {name: string; type: string}[]; rows: unknown[][]; row_count: number;
  truncated: boolean; execution_ms: number; query_id: string;
  stats: {elapsed_ms: number; cpu_ms: number; processed_rows: number; processed_bytes: number; state: string};
}
export const activeJob = (job: Job) => !['COMPLETED','FAILED'].includes(job.status);
export function statusLabel(job: Job): string {
  if (job.error_code === 'SUBMISSION_UNCERTAIN') return 'Soumission incertaine';
  return ({QUEUED:'En attente',SUBMITTING:'Soumission',RUNNING:'En cours',REGISTERING:'Catalogue',COMPLETED:'Terminée',FAILED:'En erreur'})[job.status];
}
export function jobError(job: Job): string {
  switch(job.error_code) {
    case 'SUBMISSION_UNCERTAIN': return 'La soumission à Spark n’a pas pu être confirmée. Vérifiez les traitements Spark avant de lancer une nouvelle extraction pour éviter un doublon.';
    case 'CATALOG_REGISTRATION_FAILED': return 'Les fichiers peuvent être présents, mais leur inscription au catalogue a échoué. Le service attend la disponibilité du catalogue pour finaliser le nettoyage.';
    case 'EXECUTION_TIMEOUT': return 'Le délai d’exécution du traitement est dépassé.';
    case 'RESULT_INVALID': case 'RESULT_MISSING': return 'Le résultat de l’extraction est absent ou invalide. Consultez les journaux du worker.';
    default: return 'L’extraction a échoué. Consultez les journaux du worker avec l’identifiant du traitement.';
  }
}
