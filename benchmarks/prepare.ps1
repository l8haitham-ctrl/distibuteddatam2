$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
New-Item -ItemType Directory -Force benchmarks/fixtures,benchmarks/results | Out-Null
docker compose cp sql/benchmark-fixture.sql sqlserver:/tmp/benchmark-fixture.sql
if ($LASTEXITCODE -ne 0) { throw 'Could not copy SQL fixture' }
docker compose cp sql/benchmark-fixture.sh sqlserver:/tmp/benchmark-fixture.sh
if ($LASTEXITCODE -ne 0) { throw 'Could not copy SQL fixture launcher' }
$sqlOutput = docker compose exec -T sqlserver bash /tmp/benchmark-fixture.sh
if ($LASTEXITCODE -ne 0) { throw 'Fixture initialization failed. Existing data was not replaced.' }
$rawOutput = $sqlOutput -join "`n"
$jsonStart = $rawOutput.IndexOf('{'); $jsonEnd = $rawOutput.LastIndexOf('}')
if ($jsonStart -lt 0 -or $jsonEnd -lt $jsonStart) { throw 'Missing SQL reference JSON' }
$reference = $rawOutput.Substring($jsonStart,$jsonEnd-$jsonStart+1) | ConvertFrom-Json
if ($reference.row_count -ne 100000) { throw 'Unexpected reference row count' }
$utf8 = [System.Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText((Join-Path (Get-Location) 'benchmarks/fixtures/reference.json'),($reference | ConvertTo-Json -Depth 6),$utf8)
$dockerInfo = docker info --format '{{json .}}' | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Docker unavailable' }
$systemInfo = [ordered]@{
    recorded_at=[DateTime]::UtcNow.ToString('o')
    docker_version=$dockerInfo.ServerVersion; docker_cpus=$dockerInfo.NCPU
    docker_memory_bytes=$dockerInfo.MemTotal; docker_os=$dockerInfo.OSType; architecture=$dockerInfo.Architecture
    source='SQL Server BusinessBenchmark.benchmark.Invoice100k'
    spark_image=(docker image inspect distributed-data-spark:local --format '{{.Id}}')
    trino_image=(docker image inspect distributed-data-trino-analytics:local --format '{{.Id}}')
    backend_image=(docker image inspect distributed-data-backend:local --format '{{.Id}}')
    benchmark_image=(docker image inspect distributed-data-benchmark:local --format '{{.Id}}')
    executor_memory='512m'; executor_cores=1; jdbc_readers=2; max_records_per_file=100000
}
[IO.File]::WriteAllText((Join-Path (Get-Location) 'benchmarks/fixtures/system.json'),($systemInfo | ConvertTo-Json -Depth 4),$utf8)
Write-Output 'Reference and environment prepared. No existing business rows were replaced.'
