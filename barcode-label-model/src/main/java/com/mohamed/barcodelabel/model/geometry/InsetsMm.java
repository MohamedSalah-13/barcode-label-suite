package com.mohamed.barcodelabel.model.geometry;

import static com.mohamed.barcodelabel.model.internal.Checks.requireNonNegative;

public record InsetsMm(double top, double right, double bottom, double left) {
    public static final InsetsMm ZERO = new InsetsMm(0, 0, 0, 0);

    public InsetsMm {
        requireNonNegative(top, "top");
        requireNonNegative(right, "right");
        requireNonNegative(bottom, "bottom");
        requireNonNegative(left, "left");
    }
}

