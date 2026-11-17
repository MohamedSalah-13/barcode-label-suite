package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

import static com.mohamed.barcodelabel.model.internal.Checks.requireNonNegative;
import static com.mohamed.barcodelabel.model.internal.Checks.requirePositive;

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
        requirePositive(moduleWidthMm, "moduleWidthMm");
        requireNonNegative(quietZoneMm, "quietZoneMm");
    }
}

