param([string]$JavaHome = 'C:\Program Files\Java\jdk-21', [int]$Forks = 3)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$env:JAVA_HOME = $JavaHome
# Run mvnw.cmd -DskipTests install first when production sources have changed.
& .\mvnw.cmd -pl barcode-label-fx-batch dependency:build-classpath '-Dmdep.outputFile=target/benchmark-classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'Maven classpath failed' }
$out = 'target/batch-benchmark'
New-Item -ItemType Directory -Force $out | Out-Null
$cp = 'barcode-label-fx-batch/target/classes;' + (Get-Content 'barcode-label-fx-batch/target/benchmark-classpath.txt' -Raw).Trim()
& "$JavaHome/bin/javac.exe" -encoding UTF-8 -cp $cp -d $out scripts/benchmarks/BatchBenchmark.java
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$JavaHome/bin/java.exe" -version 2>&1 | Out-File "$out/environment.txt"
git rev-parse HEAD | Out-File "$out/environment.txt" -Append
git status --short | Out-File "$out/environment.txt" -Append
# Identify uncommitted source (including new Java files), not just the base commit.
Get-ChildItem -Path 'barcode-label-*/src/main/java' -Recurse -Filter '*.java' |
    Sort-Object FullName | Get-FileHash -Algorithm SHA256 |
    Select-Object Hash,Path | ConvertTo-Json | Set-Content "$out/source-hashes.json" -Encoding utf8
Get-CimInstance Win32_Processor | Select-Object Name,NumberOfCores,NumberOfLogicalProcessors | Out-File "$out/environment.txt" -Append
Get-CimInstance Win32_ComputerSystem | Select-Object TotalPhysicalMemory | Out-File "$out/environment.txt" -Append
$results = "$out/results.jsonl"
Set-Content $results ''
$cases = @(@(1000,1,1000),@(1000,100,1000),@(10000,1,1000),@(10000,10,1000),@(10000,100,1000),@(10000,10,10000))
foreach ($case in $cases) {
    for ($fork=1; $fork -le $Forks; $fork++) {
        Write-Host "Measuring rows=$($case[0]) quantity=$($case[1]) distinct=$($case[2]) fork=$fork"
        & "$JavaHome/bin/java.exe" -Xms2g -Xmx2g -XX:+UseG1GC '-Djava.awt.headless=true' -cp "$out;$cp" com.mohamed.barcodelabel.fx.batch.BatchBenchmark $case[0] $case[1] $case[2] $fork | Add-Content $results
        if ($LASTEXITCODE -ne 0) { throw 'Benchmark failed' }
    }
}
