package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

public record TextElement(
        ElementFrame frame,
        String textTemplate,
        String fontFamily,
        double fontSizePoints,
        String color,
        boolean bold,
        boolean italic,
        TextAlignment alignment
) implements LabelElement {
    public TextElement {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(textTemplate, "textTemplate");
        Objects.requireNonNull(fontFamily, "fontFamily");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(alignment, "alignment");
        if (!Double.isFinite(fontSizePoints) || fontSizePoints <= 0) {
            throw new IllegalArgumentException("fontSizePoints must be a finite positive number");
        }
    }
}

