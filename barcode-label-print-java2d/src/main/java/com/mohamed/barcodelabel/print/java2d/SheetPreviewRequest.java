package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.LabelTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SheetPreviewRequest(
        LabelTemplate template,
        List<Map<String, ?>> records,
        int startSlotIndex,
        int pageIndex,
        double dpi,
        boolean showGuides
) {
    public SheetPreviewRequest {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(records, "records");
        records = List.copyOf(records);
        if (startSlotIndex < 0 || pageIndex < 0) throw new IllegalArgumentException("indices must be non-negative");
        if (!Double.isFinite(dpi) || dpi < 36 || dpi > 600) throw new IllegalArgumentException("dpi must be between 36 and 600");
    }
}

