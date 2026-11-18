"""Compare phase medians for matching batch benchmark scenarios."""
import json
import statistics
import sys
from collections import defaultdict
from pathlib import Path

def load(path):
    groups = defaultdict(list)
    for line in Path(path).read_text(encoding='utf-8-sig').splitlines():
        if line.strip():
            row = json.loads(line)
            groups[(row['rows'], row['quantity'], row['uniqueBarcodes'], row['phase'])].append(row)
    return groups

before, after = map(load, sys.argv[1:3])
if before.keys() != after.keys():
    raise SystemExit('Scenario/phase sets differ; do not compare incomplete runs')
print('| Rows | Copies | Distinct codes | Phase | ms before | ms after | Retained MiB before | Retained MiB after | Allocated MiB before | Allocated MiB after |')
print('|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|')
for key, old in before.items():
    new = after[key]
    if len(old) != len(new):
        raise SystemExit(f'Fork counts differ: {key}')
    med = lambda runs, metric: statistics.median(row[metric] for row in runs)
    rows, copies, distinct, phase = key
    print(f'| {rows} | {copies} | {distinct} | {phase} | {med(old,"ms"):.1f} | {med(new,"ms"):.1f} | '
          f'{med(old,"retainedDeltaBytes")/2**20:.2f} | {med(new,"retainedDeltaBytes")/2**20:.2f} | '
          f'{med(old,"allocatedBytes")/2**20:.2f} | {med(new,"allocatedBytes")/2**20:.2f} |')
