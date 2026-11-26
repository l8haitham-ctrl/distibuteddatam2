param([string]$Root=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
$path=Join-Path $Root '.env'
if(-not(Test-Path -LiteralPath $path)){throw 'Create .env first with scripts/setup-env.ps1.'}
$content=[IO.File]::ReadAllText($path)
$generator=[Security.Cryptography.RandomNumberGenerator]::Create()
try {
  foreach($key in @('CLICKHOUSE_ADMIN_PASSWORD','CLICKHOUSE_READER_PASSWORD')) {
    $pattern='(?m)^'+$key+'=(.*)$'
    $setting=[regex]::Match($content,$pattern)
    if($setting.Success) {
      if($setting.Groups[1].Value.Trim().Length -lt 16){throw "$key exists but needs a strong value. Set it privately in .env."}
      continue
    }
    $bytes=New-Object byte[] 24
    $generator.GetBytes($bytes)
    $secret=[BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
    $content+="`r`n$key=$secret`r`n"
  }
} finally {$generator.Dispose()}
[IO.File]::WriteAllText($path,$content,[Text.UTF8Encoding]::new($false))
Write-Output 'ClickHouse settings ready. Existing credentials preserved; no secrets printed.'
