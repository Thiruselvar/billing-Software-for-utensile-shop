$ErrorActionPreference = "Stop"

$jar = "lib\mysql-connector-j-9.7.0.jar"
if (!(Test-Path $jar)) {
  throw "Missing JDBC driver at $jar"
}

if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }

$sources = Get-ChildItem -Recurse -Filter *.java -Path "src" | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) { throw "No Java sources found under src/" }

Write-Host "Compiling..." -ForegroundColor Cyan
javac -cp ".;$jar" -d "bin" $sources

Write-Host "Running..." -ForegroundColor Cyan
java -cp ".;bin;$jar" yogasri.pos.App

