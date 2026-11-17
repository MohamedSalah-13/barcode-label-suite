package com.mohamed.barcodelabel.model.geometry;

import static com.mohamed.barcodelabel.model.internal.Checks.requireFinite;
import static com.mohamed.barcodelabel.model.internal.Checks.requirePositive;

public record RectMm(double x, double y, double width, double height) {
    public RectMm {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requirePositive(width, "width");
        requirePositive(height, "height");
    }
}

