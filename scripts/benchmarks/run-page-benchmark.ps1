param([Parameter(Mandatory)][string]$OutputFile)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& .\mvnw.cmd -pl barcode-label-fx-batch dependency:build-classpath '-Dmdep.outputFile=target/page-classpath.txt'
if ($LASTEXITCODE) { throw 'Classpath failed' }
$out = 'target/page-benchmark'
New-Item -ItemType Directory -Force $out | Out-Null
$cp = 'barcode-label-fx-batch/target/classes;' + (Get-Content barcode-label-fx-batch/target/page-classpath.txt -Raw).Trim()
& "$env:JAVA_HOME/bin/javac" -encoding UTF-8 -cp $cp -d $out scripts/benchmarks/BatchBenchmark.java scripts/benchmarks/PageBenchmark.java
if ($LASTEXITCODE) { throw 'Compilation failed' }
$results = foreach ($fork in 1..3) {
    & "$env:JAVA_HOME/bin/java" -Xms2g -Xmx2g -XX:+UseG1GC '-Djava.awt.headless=true' -cp "$out;$cp" com.mohamed.barcodelabel.fx.batch.PageBenchmark $fork
    if ($LASTEXITCODE) { throw 'Measurement failed' }
}
$results | Set-Content $OutputFile -Encoding utf8
