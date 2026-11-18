"""Aggregate weighted JFR allocation samples under production entry points (not exact byte counts)."""
import json
import sys
from collections import Counter
from pathlib import Path

output = {}
root = Path(sys.argv[1] if len(sys.argv) > 1 else 'target/batch-benchmark')
for phase in ('prepare', 'asDataset'):
    events = json.loads((root / f'{phase}-allocations.json').read_text(encoding='utf-8-sig'))['recording']['events']
    weights, counts = Counter(), Counter()
    for event in events:
        value = event['values']
        frames = (value.get('stackTrace') or {}).get('frames', [])
        stack = [f['method']['type']['name'] + '.' + f['method']['name'] for f in frames]
        entry = 'BatchPreparationService.prepare' if phase == 'prepare' else 'BatchProductionWorkbench.asDataset'
        if not any(entry in frame for frame in stack):
            continue
        if phase == 'prepare':
            group = ('BatchPreparationResult' if any('BatchPreparationResult.' in f for f in stack)
                     else 'BarcodePreflight' if any('BarcodePreflight.' in f for f in stack) else 'other preparation')
        else:
            group = 'CsvDataset immutable copies' if any('CsvDataset.' in f for f in stack) else 'asDataset mutable rows and headers'
        weights[group] += value['weight']
        counts[group] += 1
    total = sum(weights.values())
    output[phase] = {'totalSampleWeightBytes': total, 'groups': {
        group: {'samples': counts[group], 'sampleWeightBytes': weight, 'percent': round(weight / total * 100, 2)}
        for group, weight in weights.items()}}
print(json.dumps(output, indent=2))
