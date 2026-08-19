package com.mohamed.barcodelabel.model.page;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;

import java.util.Objects;

public record PageDefinition(
        PageMode mode,
        DimensionsMm pageSize,
        InsetsMm margins,
        PageOrientation orientation,
        SheetLayout sheetLayout
) {
    private static final double FIT_TOLERANCE_MM = 0.01;

    public PageDefinition {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(pageSize, "pageSize");
        Objects.requireNonNull(margins, "margins");
        Objects.requireNonNull(orientation, "orientation");

        if (margins.left() + margins.right() >= pageSize.width()
                || margins.top() + margins.bottom() >= pageSize.height()) {
            throw new IllegalArgumentException("margins must leave a positive printable area");
        }
        if (mode == PageMode.SHEET && sheetLayout == null) {
            throw new IllegalArgumentException("sheetLayout is required for SHEET mode");
        }
        if (mode == PageMode.SHEET) {
            double availableWidth = pageSize.width() - margins.left() - margins.right();
            double availableHeight = pageSize.height() - margins.top() - margins.bottom();
            if (sheetLayout.occupiedWidth() - availableWidth > FIT_TOLERANCE_MM
                    || sheetLayout.occupiedHeight() - availableHeight > FIT_TOLERANCE_MM) {
                throw new IllegalArgumentException("sheet layout does not fit inside the page margins");
            }
        }
    }

    public static PageDefinition a4Sheet(SheetLayout layout, InsetsMm margins) {
        return new PageDefinition(
                PageMode.SHEET,
                new DimensionsMm(210, 297),
                margins,
                PageOrientation.PORTRAIT,
                layout
        );
    }

    public DimensionsMm labelSize() {
        if (mode == PageMode.SHEET) {
            return sheetLayout.labelSize();
        }
        return new DimensionsMm(
                pageSize.width() - margins.left() - margins.right(),
                pageSize.height() - margins.top() - margins.bottom()
        );
    }
}

