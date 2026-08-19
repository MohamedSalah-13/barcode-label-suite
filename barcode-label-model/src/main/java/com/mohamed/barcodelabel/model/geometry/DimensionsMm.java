package com.mohamed.barcodelabel.model.geometry;

public record DimensionsMm(double width, double height) {
    public DimensionsMm {
        requirePositive(width, "width");
        requirePositive(height, "height");
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a finite positive number");
        }
    }
}

