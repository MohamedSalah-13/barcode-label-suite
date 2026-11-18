package com.mohamed.barcodelabel.workflow.csv;

import com.mohamed.barcodelabel.workflow.data.RepeatedList;
import java.util.List;
import java.util.Map;

public record CsvDataset(List<String> headers, List<Map<String, String>> rows, char delimiter, List<Integer> sourceRows) {
    public CsvDataset(List<String> headers, List<Map<String, String>> rows, char delimiter) {
        this(headers, rows, delimiter, List.of());
    }

    public CsvDataset {
        headers = List.copyOf(headers);
        rows = RepeatedList.mapCopy(rows, Map::copyOf);
        sourceRows = RepeatedList.copyOf(sourceRows);
        if (!sourceRows.isEmpty() && sourceRows.size() != rows.size()) {
            throw new IllegalArgumentException("Each data record must have a source row");
        }
    }

    /** One-based CSV record number, including the header and skipped blank records. */
    public int sourceRow(int index) { return sourceRows.isEmpty() ? index + 2 : sourceRows.get(index); }

    public List<Map<String, ?>> printRecords() {
        return RepeatedList.mapCopy(rows, row -> row);
    }
}
