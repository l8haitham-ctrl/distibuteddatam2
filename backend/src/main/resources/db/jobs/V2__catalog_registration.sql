ALTER TABLE storage_job DROP CONSTRAINT storage_job_status_check;
ALTER TABLE storage_job ADD CONSTRAINT storage_job_status_check CHECK (status IN ('QUEUED','SUBMITTING','RUNNING','REGISTERING','COMPLETED','FAILED'));
ALTER TABLE storage_job ADD COLUMN pending_result_json text;
ALTER TABLE storage_job ADD COLUMN catalog_table text;
ALTER TABLE storage_job ADD COLUMN catalog_view text;
ALTER TABLE storage_job ADD COLUMN registration_ms bigint;
DROP INDEX storage_job_single_active_idx;
CREATE UNIQUE INDEX storage_job_single_active_idx ON storage_job((1)) WHERE status IN ('SUBMITTING','RUNNING','REGISTERING');
