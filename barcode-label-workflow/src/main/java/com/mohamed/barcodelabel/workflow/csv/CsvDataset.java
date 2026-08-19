package com.mohamed.barcodelabel.workflow.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CsvDataset(List<String> headers, List<Map<String, String>> rows, char delimiter) {
    public CsvDataset {
        headers = List.copyOf(headers);
        rows = rows.stream().map(Map::copyOf).toList();
    }

    public List<Map<String, ?>> printRecords() {
        List<Map<String, ?>> result = new ArrayList<>(rows.size());
        result.addAll(rows);
        return List.copyOf(result);
    }
}
