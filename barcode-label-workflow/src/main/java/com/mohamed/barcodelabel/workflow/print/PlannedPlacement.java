package com.mohamed.barcodelabel.workflow.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;

import java.util.Objects;

public record PlannedPlacement(int recordIndex, int slotIndex, RectMm bounds) {
    public PlannedPlacement {
        if (recordIndex < 0) throw new IllegalArgumentException("recordIndex must not be negative");
        if (slotIndex < 0) throw new IllegalArgumentException("slotIndex must not be negative");
        Objects.requireNonNull(bounds, "bounds");
    }
}
