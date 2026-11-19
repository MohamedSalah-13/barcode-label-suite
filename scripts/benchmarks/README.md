# Batch preparation diagnostic harness

This harness calls the existing production methods without changing their implementation. It runs without a JavaFX toolkit and never opens a printer. Use Java 21 and build the current source before running it:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -DskipTests install
.\scripts\benchmarks\run-batch-benchmark.ps1 -JavaHome $env:JAVA_HOME -Forks 3
python scripts/benchmarks/summarize_batch.py target/batch-benchmark/results.jsonl
```

The PowerShell runner targets Windows and reads CPU/RAM through CIM. Each case runs in a separate JVM for each fork. It records Java version, source commit and hardware under `target/batch-benchmark/`. JVM flags: `-Xms2g -Xmx2g -XX:+UseG1GC -Djava.awt.headless=true`. The runner overwrites its previous output; copy results you want to retain first.

The harness generates deterministic valid records matching the sample CSV's four column names (`Name`, `Code`, `Retail Price`, `Qty`), Arabic product names, CODE_128 values and three mapped template fields. `uniqueBarcodes` controls repetition independently of source rows and copy count. The mapping allows up to five million labels; the largest measured case is one million. Templates use an A4 3×7 sheet, first slot zero. No file parsing, actual printing, full JavaFX UI, or preview rasterization is included.

Each fork warms class loading and caches with three runs of 200 rows × 10 copies and 200 distinct barcodes, including conversion, page planning and result copying. Timings use `System.nanoTime()` around one call per stage. The report uses three forks and records the spread; this is an end-to-end diagnostic, not a JMH microbenchmark or a claim about worst-case latency.

Measurements:

- `source`: construction of the in-memory `CsvDataset`, reported separately from preparation.
- `prepare`: `BatchPreparationService.prepare`, including barcode preflight, row mapping, quantity expansion, and `BatchPreparationResult` defensive copies.
- `asDataset`: the actual package-private `BatchProductionWorkbench.asDataset`; the harness shares its package to call it without copying the implementation.
- `plan`: `UniversalPagePlanner.plan`, including the lower-level sheet planner.
- `resultCopyProbe`: a separate constructor call on already prepared maps. It isolates defensive-copy cost, but is **not** an additional production stage. Its input map representation differs from the maps created inside `prepare`, so its time/memory must not be subtracted mechanically from preparation.
- `expandedPreflight`: a fresh `BarcodePreflight` over converted rows, mirroring the second preflight in the print pane; this still encodes each distinct barcode once, but resolves placeholders for every label. It excludes JavaFX task scheduling.

`allocatedBytes` uses Java 21's `com.sun.management.ThreadMXBean` for the measuring thread only. It includes temporary allocations, not native allocations or allocations on other threads. `retainedDeltaBytes` is the difference in used Java heap after explicit GC, with stage outputs deliberately retained in a static list. Source, prepared records, converted dataset and planned pages stay live together. The copy probe is removed before expanded preflight. Used-heap readings are captured before formatting output to avoid counting the output buffer/TLAB reservation. Small differences remain GC/runtime estimates, not exact object sizes. The measurements do not claim peak heap, process RSS or total desktop memory. GC counts/times inside each timed stage are recorded; explicit between-stage GCs are excluded and may make behavior differ from an uninterrupted UI session.

To inspect allocation stacks separately, launch the compiled harness with the same classpath plus `-Dbenchmark.jfr.phase=prepare` (or `asDataset`) and `-Dbenchmark.jfr.file=target/batch-benchmark/prepare.jfr`. This uses JFR's `profile` configuration. Keep those diagnostic runs separate from the timing baseline. JFR allocation samples are weighted estimates, not an exact allocation census.

For example, after running the baseline (the compiled classes and classpath are reused):

```powershell
$cp = 'target/batch-benchmark;barcode-label-fx-batch/target/classes;' + (Get-Content barcode-label-fx-batch/target/benchmark-classpath.txt -Raw).Trim()
foreach ($phase in @('prepare', 'asDataset')) {
    & "$env:JAVA_HOME/bin/java.exe" -Xms2g -Xmx2g -XX:+UseG1GC '-Djava.awt.headless=true' "-Dbenchmark.jfr.phase=$phase" "-Dbenchmark.jfr.file=target/batch-benchmark/$phase.jfr" -cp $cp com.mohamed.barcodelabel.fx.batch.BatchBenchmark 10000 100 1000 0 > "target/batch-benchmark/$phase-profile.jsonl"
    & "$env:JAVA_HOME/bin/jfr.exe" print --json --events jdk.ObjectAllocationSample --stack-depth 64 "target/batch-benchmark/$phase.jfr" > "target/batch-benchmark/$phase-allocations.json"
}
python scripts/benchmarks/summarize_allocations.py target/batch-benchmark
```

Use `liveHeapBytes` at the end of `plan` for the retained primary pipeline. The isolated result-copy probe can also initialize cached views on its input maps; removing the probe output does not necessarily undo those effects. Later total-heap readings are diagnostic only, not a baseline for the unmodified UI pipeline.

To compare a new run with the saved baseline:

```powershell
python scripts/benchmarks/compare_batch.py docs/benchmarks/batch-baseline-2026-11-18.jsonl target/batch-benchmark/results.jsonl
```

The runner also saves `source-hashes.json` and Git status so a run on uncommitted production changes can be identified. Snapshot those files with the results before another run overwrites `target/batch-benchmark` or `clean` deletes it. The benchmark Java source, input generation, scenario matrix and measurement settings were unchanged between the saved baseline and optimized runs.


For the barcode-cache experiment, `prepare` starts with a fresh `BarcodeValidationCache` and
`expandedPreflight` shares that cache, matching the composed desktop workbench. The cache remains
strongly reachable for the whole measurement, including the last post-GC reading. Each warm-up
iteration uses its own preparation service, preserving the original cold encoding work during
warm-up; warm-up diagnostics never enter the measured cache. Input generation, six scenarios,
three forks and JVM flags remain unchanged. Compare this run with
`docs/benchmarks/batch-optimized-2026-11-18.jsonl` (the compressed-record implementation).
The second check still resolves every expanded row, including blank-field checks and current row
provenance; this experiment measures reuse of encoding diagnostics, not skipping CSV validation.

For the logical-row preflight run, compare against `docs/benchmarks/barcode-cache-2026-11-18.jsonl`.
The phase name `expandedPreflight` stays the same for the comparison script. It now calls the
production preflight on the compact records and original CSV row numbers from the prepared batch.
All six scenarios use the previous inputs, warm-up, JVM flags and three independent forks.
