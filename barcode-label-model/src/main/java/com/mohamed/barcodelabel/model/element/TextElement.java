package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

import static com.mohamed.barcodelabel.model.internal.Checks.requirePositive;

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
        requirePositive(fontSizePoints, "fontSizePoints");
    }
}

