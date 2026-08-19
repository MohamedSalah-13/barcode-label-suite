package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

public record BarcodeElement(
        ElementFrame frame,
        String dataTemplate,
        Symbology symbology,
        double moduleWidthMm,
        double quietZoneMm,
        boolean showHumanReadableText
) implements LabelElement {
    public BarcodeElement {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(dataTemplate, "dataTemplate");
        Objects.requireNonNull(symbology, "symbology");
        requireNonNegative(moduleWidthMm, "moduleWidthMm", false);
        requireNonNegative(quietZoneMm, "quietZoneMm", true);
    }

    private static void requireNonNegative(double value, String name, boolean allowZero) {
        boolean invalidRange = allowZero ? value < 0 : value <= 0;
        if (!Double.isFinite(value) || invalidRange) {
            throw new IllegalArgumentException(name + (allowZero ? " must be non-negative" : " must be positive"));
        }
    }
}

