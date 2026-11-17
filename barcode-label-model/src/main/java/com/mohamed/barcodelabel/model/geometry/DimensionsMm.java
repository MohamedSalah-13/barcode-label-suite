package com.mohamed.barcodelabel.model.geometry;

import static com.mohamed.barcodelabel.model.internal.Checks.requirePositive;

public record DimensionsMm(double width, double height) {
    public DimensionsMm {
        requirePositive(width, "width");
        requirePositive(height, "height");
    }
}

