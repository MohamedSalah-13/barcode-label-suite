package com.mohamed.barcodelabel.batch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public static ColumnMapping automatic(List<String> templateFields, List<String> headers) {
        var mapping = new LinkedHashMap<String, String>();
        for (String field : templateFields) {
            headers.stream().filter(header -> normalized(header).equals(normalized(field))).findFirst()
                    .ifPresent(header -> mapping.put(field, header));
        }
        return new ColumnMapping(mapping, null, 1, 999, 100_000);
    }

    private static String normalized(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }
}
