package com.mohamed.barcodelabel.model.page;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;

import java.util.Objects;

import static com.mohamed.barcodelabel.model.internal.Checks.requireNonNegative;
import static com.mohamed.barcodelabel.model.internal.Checks.requirePositive;

public record SheetLayout(
        int columns,
        int rows,
        DimensionsMm labelSize,
        double horizontalGap,
        double verticalGap
) {
    public SheetLayout {
        requirePositive(columns, "columns");
        requirePositive(rows, "rows");
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
}

