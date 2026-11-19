package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.print.SheetPlan;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.SheetLayoutEngine;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

final class SheetPrintable implements Printable {
    private final LabelPrintRequest request;
    private final double renderDpi;
    private final BufferedImageLabelRenderer renderer;
    private final SheetPlan pages;

    SheetPrintable(LabelPrintRequest request, double renderDpi) {
        this(request, renderDpi, new BufferedImageLabelRenderer(), new SheetLayoutEngine());
    }

    SheetPrintable(LabelPrintRequest request, double renderDpi, BufferedImageLabelRenderer renderer, SheetLayoutEngine engine) {
        this.request = request;
        this.renderDpi = renderDpi;
        this.renderer = renderer;
        this.pages = engine.layout(request.template().page(), request.records().size(), request.startSlotIndex());
    }

    int pageCount() {
        return pages.pageCount();
    }

    /** Total label placements across the 0-based, inclusive page-index range {@code [fromPageIndex, toPageIndex]}. */
    int placementCount(int fromPageIndex, int toPageIndex) {
        return pages.placementCount(fromPageIndex, toPageIndex);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex < 0 || pageIndex >= pages.pageCount()) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.translate(UnitConversions.mmToPoints(request.offset().horizontalMm()),
                    UnitConversions.mmToPoints(request.offset().verticalMm()));
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (var placement : pages.pageAt(pageIndex).placements()) {
                var label = renderer.render(new RenderRequest(request.template(),
                        request.records().get(placement.recordIndex()), renderDpi));
                double x = UnitConversions.mmToPoints(placement.bounds().x());
                double y = UnitConversions.mmToPoints(placement.bounds().y());
                double width = UnitConversions.mmToPoints(placement.bounds().width());
                double height = UnitConversions.mmToPoints(placement.bounds().height());
                AffineTransform transform = new AffineTransform();
                transform.translate(x, y);
                transform.scale(width / label.getWidth(), height / label.getHeight());
                g2.drawImage(label, transform, null);
            }
            return PAGE_EXISTS;
        } catch (Exception exception) {
            PrinterException printerException = new PrinterException("Cannot render page " + (pageIndex + 1) + ": " + exception.getMessage());
            printerException.initCause(exception);
            throw printerException;
        } finally {
            g2.dispose();
        }
    }
}

