package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.print.java2d.SheetPageRenderer;
import com.mohamed.barcodelabel.print.java2d.SheetPreviewRequest;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.workflow.print.UniversalPagePlanner;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Measures a retained workflow plan, page lookup, and independently the rendered Java2D preview. */
public final class PageBenchmark {
    private static BufferedImage render(SheetPageRenderer renderer, SheetPreviewRequest request) {
        try { return renderer.render(request); }
        catch (RenderException ex) { throw new IllegalStateException(ex); }
    }

    public static void main(String[] args) throws Exception {
        int count = 1_000_000, start = 5;
        var template = BatchBenchmark.template();
        var planner = new UniversalPagePlanner();
        var renderer = new SheetPageRenderer();
        var record = Map.<String,Object>of("product_name", "Product", "price", "12.50", "sku", "SKU-100000");
        var records = List.<Map<String,?>>copyOf(Collections.nCopies(count, record));
        int last = (count - 1 + start) / template.page().sheetLayout().labelsPerPage();
        var firstRequest = new SheetPreviewRequest(template, records, start, 0, 96, true);
        var lastRequest = new SheetPreviewRequest(template, records, start, last, 96, true);
        for (int i = 0; i < 4; i++) {
            planner.plan(template.page(), 1000, start).getLast();
            render(renderer, new SheetPreviewRequest(template, records.subList(0, 42), start, 0, 96, true));
        }
        BatchBenchmark.rows = 10_000;
        BatchBenchmark.quantity = 100;
        BatchBenchmark.unique = 1;
        BatchBenchmark.fork = Integer.parseInt(args[0]);
        var pages = BatchBenchmark.measure("plan", () -> planner.plan(template.page(), count, start));
        var first = BatchBenchmark.measure("lookupFirst", pages::getFirst);
        var end = BatchBenchmark.measure("lookupLast", pages::getLast);
        if (pages.size() != last + 1 || first.placements().getFirst().recordIndex() != 0
                || end.placements().getLast().recordIndex() != count - 1) throw new AssertionError();
        BatchBenchmark.measure("renderFirst", () -> render(renderer, firstRequest));
        BatchBenchmark.measure("renderLast", () -> render(renderer, lastRequest));
    }
}
