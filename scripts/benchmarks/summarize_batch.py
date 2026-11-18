"""Summarize the diagnostic JSONL; timings are medians of independent JVMs, not a microbenchmark score."""
import json
import statistics
import sys
from collections import defaultdict
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else 'target/batch-benchmark/results.jsonl')
groups = defaultdict(list)
for line in path.read_text(encoding='utf-8-sig').splitlines():
    if line.strip():
        row = json.loads(line)
        groups[(row['rows'], row['quantity'], row['uniqueBarcodes'], row['phase'])].append(row)
print('| Rows | Copies/row | Distinct barcodes | Phase | Forks | Median ms (min-max) | Allocated MiB | Retained delta MiB | Live heap MiB |')
print('|---:|---:|---:|---|---:|---:|---:|---:|---:|')
for (rows, copies, distinct, phase), runs in groups.items():
    times = [run['ms'] for run in runs]
    mib = lambda key: statistics.median(run[key] for run in runs) / 2**20
    print(f'| {rows} | {copies} | {distinct} | {phase} | {len(runs)} | '
          f'{statistics.median(times):.1f} ({min(times):.1f}-{max(times):.1f}) | '
          f'{mib("allocatedBytes"):.2f} | {mib("retainedDeltaBytes"):.2f} | {mib("liveHeapBytes"):.2f} |')
