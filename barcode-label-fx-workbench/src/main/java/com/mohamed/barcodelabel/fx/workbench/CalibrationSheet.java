package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.print.LabelPrintRequest;

import java.util.List;
import java.util.Map;

/** One uncorrected reference cross, independent of the label design and batch. */
final class CalibrationSheet {
    private CalibrationSheet() {}

    static double referenceMm(double edgeMm) { return Math.min(10, edgeMm / 2); }

    static LabelPrintRequest request(String printer, PageDefinition stock, String title) {
        var size = stock.pageSize();
        double x = referenceMm(size.width());
        double y = referenceMm(size.height());
        double arm = Math.min(4, Math.min(x, y) / 2);
        double stroke = Math.min(0.3, arm / 4);
        // Rasterize just the mark, keeping the physical page size and orientation of the selected stock.
        var margins = new InsetsMm(y - arm, size.width() - x - arm, size.height() - y - arm, x - arm);
        var page = new PageDefinition(PageMode.SINGLE, size, margins, stock.orientation(), null);
        var horizontal = mark(new RectMm(0, arm - stroke / 2, arm * 2, stroke), title);
        var vertical = mark(new RectMm(arm - stroke / 2, 0, stroke, arm * 2), title);
        var template = LabelTemplate.create(title, page, List.of(horizontal, vertical));
        return new LabelPrintRequest(template, List.of(Map.of()), printer, 1, 0);
    }

    private static ShapeElement mark(RectMm bounds, String title) {
        return new ShapeElement(ElementFrame.create(title, bounds, 0), ShapeType.RECTANGLE, "black", "black", 0);
    }
}
