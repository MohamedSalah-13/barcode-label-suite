package com.mohamed.barcodelabel.model.geometry;

public record RectMm(double x, double y, double width, double height) {
    public RectMm {
        requireFinite(x, "x");
        requireFinite(y, "y");
        if (!Double.isFinite(width) || width <= 0 || !Double.isFinite(height) || height <= 0) {
            throw new IllegalArgumentException("width and height must be finite positive numbers");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}

