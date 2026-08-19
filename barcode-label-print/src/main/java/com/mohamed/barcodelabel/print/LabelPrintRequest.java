package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.LabelTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LabelPrintRequest(
        LabelTemplate template,
        List<Map<String, ?>> records,
        String printerName,
        int copies,
        int startSlotIndex
) {
    public LabelPrintRequest {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(printerName, "printerName");
        records = List.copyOf(records);
        if (copies <= 0) {
            throw new IllegalArgumentException("copies must be positive");
        }
        if (startSlotIndex < 0) {
            throw new IllegalArgumentException("startSlotIndex must be non-negative");
        }
    }
}

