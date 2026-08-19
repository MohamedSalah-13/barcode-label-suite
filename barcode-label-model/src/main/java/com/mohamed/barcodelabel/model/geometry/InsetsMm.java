package com.mohamed.barcodelabel.model.geometry;

public record InsetsMm(double top, double right, double bottom, double left) {
    public static final InsetsMm ZERO = new InsetsMm(0, 0, 0, 0);

    public InsetsMm {
        requireNonNegative(top, "top");
        requireNonNegative(right, "right");
        requireNonNegative(bottom, "bottom");
        requireNonNegative(left, "left");
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}

