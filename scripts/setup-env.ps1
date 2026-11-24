param([string]$Path='.env',[string]$Root=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
$destination=if([IO.Path]::IsPathRooted($Path)){$Path}else{Join-Path $Root $Path}
if(Test-Path -LiteralPath $destination){Write-Output 'Existing configuration preserved; no secret was changed.';exit 0}
$template=[IO.File]::ReadAllText((Join-Path $Root '.env.example'))
$generator=[Security.Cryptography.RandomNumberGenerator]::Create()
try{
  foreach($key in @('APP_LOGIN_PASSWORD','MSSQL_SA_PASSWORD','LAKE_READER_PASSWORD','MINIO_ROOT_PASSWORD','STORAGE_SECRET_KEY','POSTGRES_ADMIN_PASSWORD','JOBS_PASSWORD','METASTORE_PASSWORD','TRINO_S3_SECRET_KEY','TRINO_REGISTRAR_PASSWORD','TRINO_READER_PASSWORD','TRINO_KEYSTORE_PASSWORD','TRINO_SHARED_SECRET','PG_SOURCE_ADMIN_PASSWORD','PG_SOURCE_READER_PASSWORD','REDIS_ADMIN_PASSWORD','REDIS_READER_PASSWORD')){
    $bytes=New-Object byte[] 32;$generator.GetBytes($bytes)
    $secret='Aa1!'+([BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant())
    if($key -eq 'TRINO_SHARED_SECRET'){$secret=[Convert]::ToBase64String($bytes)}
    $pattern='(?m)^'+[regex]::Escape($key)+'=.*$'
    if([regex]::Matches($template,$pattern).Count -ne 1){throw "Missing or duplicated setting: $key"}
    $template=[regex]::Replace($template,$pattern,($key+'='+$secret))
  }
}finally{$generator.Dispose()}
$parent=Split-Path $destination -Parent
if(-not(Test-Path -LiteralPath $parent)){New-Item -ItemType Directory -Force -Path $parent | Out-Null}
[IO.File]::WriteAllText($destination,$template,[Text.UTF8Encoding]::new($false))
Write-Output 'New local configuration created. Secrets were not printed. Keep this file private and retain it with its Docker volumes.'
