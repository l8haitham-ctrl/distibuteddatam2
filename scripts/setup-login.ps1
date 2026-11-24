param([string]$Root=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
$path=Join-Path $Root '.env'
if(-not(Test-Path -LiteralPath $path)){throw 'Create .env first with scripts/setup-env.ps1.'}
$content=[IO.File]::ReadAllText($path)
$password=[regex]::Match($content,'(?m)^APP_LOGIN_PASSWORD=(.*)$')
if($password.Success){
  if($password.Groups[1].Value.Trim().Length -lt 16){throw 'APP_LOGIN_PASSWORD exists but needs at least 16 characters. Set it privately in .env.'}
  Write-Output 'Existing platform login preserved. No secret was changed.';exit 0
}
$bytes=New-Object byte[] 24
$generator=[Security.Cryptography.RandomNumberGenerator]::Create()
try{$generator.GetBytes($bytes)}finally{$generator.Dispose()}
$secret=[BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
if(-not[regex]::IsMatch($content,'(?m)^APP_LOGIN_USERNAME=')){$content+="`r`nAPP_LOGIN_USERNAME=admin"}
$content+="`r`nAPP_LOGIN_PASSWORD=$secret`r`n"
[IO.File]::WriteAllText($path,$content,[Text.UTF8Encoding]::new($false))
Write-Output 'Platform login added to .env. Password was not printed; existing service credentials were preserved.'
