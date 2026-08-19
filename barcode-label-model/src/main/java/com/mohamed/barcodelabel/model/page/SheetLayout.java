package com.mohamed.barcodelabel.model.page;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;

import java.util.Objects;

public record SheetLayout(
        int columns,
        int rows,
        DimensionsMm labelSize,
        double horizontalGap,
        double verticalGap
) {
    public SheetLayout {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException("columns and rows must be positive");
        }
        Objects.requireNonNull(labelSize, "labelSize");
        requireNonNegative(horizontalGap, "horizontalGap");
        requireNonNegative(verticalGap, "verticalGap");
    }

    public int labelsPerPage() {
        return Math.multiplyExact(columns, rows);
    }

    public double occupiedWidth() {
        return columns * labelSize.width() + (columns - 1) * horizontalGap;
    }

    public double occupiedHeight() {
        return rows * labelSize.height() + (rows - 1) * verticalGap;
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}

