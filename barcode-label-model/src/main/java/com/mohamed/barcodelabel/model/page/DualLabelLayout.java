package com.mohamed.barcodelabel.model.page;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import java.util.List;
import java.util.Objects;
import static com.mohamed.barcodelabel.model.internal.Checks.*;

/** Two placements of one design, in millimetres relative to the complete label.
 * A null split position follows the centre when the label is resized.
 * The guide is editor metadata and must never be rendered for printing. */
public record DualLabelLayout(InsetsMm margins, Double splitPositionMm, double gapMm,
                              boolean showGuide, String guideColor, boolean dashedGuide) {
    public DualLabelLayout {
        Objects.requireNonNull(margins, "margins");
        if (splitPositionMm != null) requirePositive(splitPositionMm, "splitPositionMm");
        requireNonNegative(gapMm, "gapMm");
        if (guideColor == null || !guideColor.matches("#[0-9a-fA-F]{6}"))
            throw new IllegalArgumentException("guideColor must be #RRGGBB");
    }

    public static DualLabelLayout standard() {
        return new DualLabelLayout(new InsetsMm(0.5, 1, 0.5, 1), null, 1, true, "#94A3B8", true);
    }

    public double splitAt(DimensionsMm size) {
        return splitPositionMm == null ? size.height() / 2 : splitPositionMm;
    }

    /** Equal content areas fit both halves even when the division is off centre. */
    public List<RectMm> areas(DimensionsMm size) {
        double split = splitAt(size);
        double width = size.width() - margins.left() - margins.right();
        double height = Math.min(split - gapMm / 2 - margins.top(),
                size.height() - margins.bottom() - split - gapMm / 2);
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("dual layout must leave space in both halves");
        return List.of(new RectMm(margins.left(), margins.top(), width, height),
                new RectMm(margins.left(), split + gapMm / 2, width, height));
    }
}
