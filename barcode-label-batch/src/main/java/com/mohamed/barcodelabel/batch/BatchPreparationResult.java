package com.mohamed.barcodelabel.batch;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BatchPreparationResult(int sourceRows, List<Map<String, ?>> printRecords,
                                     List<BatchValidationIssue> issues) {
    public BatchPreparationResult {
        // Map.copyOf throws NullPointerException if any value is null; a defensive per-row copy
        // via LinkedHashMap tolerates null values while still fully decoupling from the caller's map.
        printRecords = printRecords.stream()
                .<Map<String, ?>>map(record -> Collections.unmodifiableMap(new LinkedHashMap<>(record)))
                .toList();
        issues = List.copyOf(issues);
    }

    public int labelCount() { return printRecords.size(); }
    public boolean ready() { return issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR); }
}
