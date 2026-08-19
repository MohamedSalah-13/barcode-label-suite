package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

public record ShapeElement(
        ElementFrame frame,
        ShapeType shapeType,
        String strokeColor,
        String fillColor,
        double strokeWidthMm
) implements LabelElement {
    public ShapeElement {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(shapeType, "shapeType");
        Objects.requireNonNull(strokeColor, "strokeColor");
        Objects.requireNonNull(fillColor, "fillColor");
        if (!Double.isFinite(strokeWidthMm) || strokeWidthMm < 0) {
            throw new IllegalArgumentException("strokeWidthMm must be finite and non-negative");
        }
    }
}

