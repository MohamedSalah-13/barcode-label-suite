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
        int startSlotIndex,
        int firstPage,
        int lastPage,
        PrintOffset offset
) {
    public LabelPrintRequest {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(printerName, "printerName");
        Objects.requireNonNull(offset, "offset");
        records = List.copyOf(records);
        if (copies <= 0) {
            throw new IllegalArgumentException("copies must be positive");
        }
        if (startSlotIndex < 0) {
            throw new IllegalArgumentException("startSlotIndex must be non-negative");
        }
        if (firstPage < 0) {
            throw new IllegalArgumentException("firstPage must be non-negative (0 means \"from the first page\")");
        }
        if (lastPage < 0) {
            throw new IllegalArgumentException("lastPage must be non-negative (0 means \"through the last page\")");
        }
        if (firstPage > 0 && lastPage > 0 && firstPage > lastPage) {
            throw new IllegalArgumentException("firstPage must not exceed lastPage");
        }
    }

    public LabelPrintRequest(LabelTemplate template, List<Map<String, ?>> records, String printerName,
            int copies, int startSlotIndex, int firstPage, int lastPage) {
        this(template, records, printerName, copies, startSlotIndex, firstPage, lastPage, PrintOffset.ZERO);
    }

    public LabelPrintRequest withOffset(PrintOffset offset) {
        return new LabelPrintRequest(template, records, printerName, copies, startSlotIndex, firstPage, lastPage, offset);
    }

    /** Prints every planned page, starting from {@code startSlotIndex}. */
    public LabelPrintRequest(LabelTemplate template, List<Map<String, ?>> records, String printerName,
            int copies, int startSlotIndex) {
        this(template, records, printerName, copies, startSlotIndex, 0, 0);
    }
}

