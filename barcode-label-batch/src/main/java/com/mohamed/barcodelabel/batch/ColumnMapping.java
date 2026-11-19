package com.mohamed.barcodelabel.batch;

import java.util.List;
import java.util.Map;

public record ColumnMapping(Map<String, String> fields, String quantityColumn,
                            int defaultCopies, int maxCopiesPerRow, int maxTotalLabels) {
    public ColumnMapping {
        fields = Map.copyOf(fields);
        if (fields.entrySet().stream().anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank())) {
            throw new IllegalArgumentException("Field mappings must not be blank");
        }
        if (quantityColumn != null && quantityColumn.isBlank()) quantityColumn = null;
        if (defaultCopies <= 0) throw new IllegalArgumentException("defaultCopies must be positive");
        if (maxCopiesPerRow <= 0) throw new IllegalArgumentException("maxCopiesPerRow must be positive");
        if (maxTotalLabels <= 0) throw new IllegalArgumentException("maxTotalLabels must be positive");
    }

    /** CSV values with the template field aliases applied. */
    public Map<String, String> resolveRow(Map<String, String> source) {
        var resolved = new java.util.LinkedHashMap<>(source);
        fields.forEach((field, column) -> resolved.put(field, source.getOrDefault(column, "")));
        return resolved;
    }

    /**
     * The mapping the CSV's column names suggest: a likely column for each field, and the quantity column if
     * one is named like one (see {@link ColumnMatcher}). The user reviews it before printing.
     */
    public static ColumnMapping automatic(List<String> templateFields, List<String> headers) {
        return new ColumnMapping(ColumnMatcher.fields(templateFields, headers), ColumnMatcher.quantityColumn(headers),
                1, 999, 100_000);
    }
}
