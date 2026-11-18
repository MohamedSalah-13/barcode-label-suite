package com.mohamed.barcodelabel.workflow.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CsvDataset(List<String> headers, List<Map<String, String>> rows, char delimiter, List<Integer> sourceRows) {
    public CsvDataset(List<String> headers, List<Map<String, String>> rows, char delimiter) {
        this(headers, rows, delimiter, List.of());
    }

    public CsvDataset {
        headers = List.copyOf(headers);
        rows = rows.stream().map(Map::copyOf).toList();
        sourceRows = List.copyOf(sourceRows);
        if (!sourceRows.isEmpty() && sourceRows.size() != rows.size()) {
            throw new IllegalArgumentException("Each data record must have a source row");
        }
    }

    /** One-based CSV record number, including the header and skipped blank records. */
    public int sourceRow(int index) { return sourceRows.isEmpty() ? index + 2 : sourceRows.get(index); }

    public List<Map<String, ?>> printRecords() {
        List<Map<String, ?>> result = new ArrayList<>(rows.size());
        result.addAll(rows);
        return List.copyOf(result);
    }
}
