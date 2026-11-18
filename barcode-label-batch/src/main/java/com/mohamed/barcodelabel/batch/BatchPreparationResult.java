package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.workflow.data.RepeatedList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BatchPreparationResult(int sourceRows, List<Map<String, ?>> printRecords,
                                     List<BatchValidationIssue> issues, List<Integer> printSourceRows) {
    /** Existing callers may supply expanded records without source-row metadata. */
    public BatchPreparationResult(int sourceRows, List<Map<String, ?>> printRecords, List<BatchValidationIssue> issues) {
        this(sourceRows, printRecords, issues, List.of());
    }

    public BatchPreparationResult {
        // Map.copyOf throws NullPointerException if any value is null; a defensive per-row copy
        // via LinkedHashMap tolerates null values while still fully decoupling from the caller's map.
        printRecords = RepeatedList.mapCopy(printRecords,
                record -> Collections.unmodifiableMap(new LinkedHashMap<>(record)));
        issues = List.copyOf(issues);
        printSourceRows = RepeatedList.copyOf(printSourceRows);
        if (!printSourceRows.isEmpty() && printSourceRows.size() != printRecords.size()) {
            throw new IllegalArgumentException("Each print record must have a source row");
        }
    }

    public int labelCount() { return printRecords.size(); }
    public boolean ready() { return issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR); }
}
