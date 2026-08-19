package com.mohamed.barcodelabel.batch;

import java.util.List;
import java.util.Map;

public record BatchPreparationResult(int sourceRows, List<Map<String, ?>> printRecords,
                                     List<BatchValidationIssue> issues) {
    public BatchPreparationResult {
        printRecords = printRecords.stream().map(Map::copyOf).toList();
        issues = List.copyOf(issues);
    }

    public int labelCount() { return printRecords.size(); }
    public boolean ready() { return issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR); }
}
