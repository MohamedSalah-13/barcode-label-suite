package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

import static com.mohamed.barcodelabel.model.internal.Checks.requireNonNegative;

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
        requireNonNegative(strokeWidthMm, "strokeWidthMm");
    }
}

