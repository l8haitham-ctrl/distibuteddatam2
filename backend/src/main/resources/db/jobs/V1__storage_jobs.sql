CREATE TABLE storage_job (
    id uuid PRIMARY KEY,
    request_json text NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('QUEUED','SUBMITTING','RUNNING','COMPLETED','FAILED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    submission_id varchar(128),
    error_code varchar(64),
    result_json text,
    CHECK ((status = 'COMPLETED') = (result_json IS NOT NULL))
);
CREATE INDEX storage_job_dispatch_idx ON storage_job(status, created_at);
CREATE UNIQUE INDEX storage_job_single_active_idx ON storage_job((1))
    WHERE status IN ('SUBMITTING','RUNNING');
