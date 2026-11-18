package com.mohamed.barcodelabel.print;

/** Translation in page coordinates. Positive values move the output right and down. */
public record PrintOffset(double horizontalMm, double verticalMm) {
    public static final PrintOffset ZERO = new PrintOffset(0, 0);

    public PrintOffset {
        if (!Double.isFinite(horizontalMm) || !Double.isFinite(verticalMm)) {
            throw new IllegalArgumentException("Print offsets must be finite");
        }
    }
}
