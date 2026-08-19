package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.SheetLayoutEngine;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;

public final class SheetPageRenderer {
    private final BufferedImageLabelRenderer labelRenderer;
    private final SheetLayoutEngine layoutEngine;

    public SheetPageRenderer() {
        this(new BufferedImageLabelRenderer(), new SheetLayoutEngine());
    }

    public SheetPageRenderer(BufferedImageLabelRenderer labelRenderer, SheetLayoutEngine layoutEngine) {
        this.labelRenderer = Objects.requireNonNull(labelRenderer, "labelRenderer");
        this.layoutEngine = Objects.requireNonNull(layoutEngine, "layoutEngine");
    }

    public int pageCount(SheetPreviewRequest request) {
        return layoutEngine.plan(request.template().page(), request.records().size(), request.startSlotIndex()).size();
    }

    public BufferedImage render(SheetPreviewRequest request) throws RenderException {
        var pages = layoutEngine.plan(request.template().page(), request.records().size(), request.startSlotIndex());
        if (request.pageIndex() >= pages.size()) {
            throw new RenderException("Page index " + request.pageIndex() + " is outside " + pages.size() + " pages");
        }
        var pageDefinition = request.template().page();
        int pageWidth = pixels(pageDefinition.pageSize().width(), request.dpi());
        int pageHeight = pixels(pageDefinition.pageSize().height(), request.dpi());
        BufferedImage pageImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = pageImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, pageWidth, pageHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (request.showGuides()) drawGuides(graphics, request);
            for (var placement : pages.get(request.pageIndex()).placements()) {
                BufferedImage label = labelRenderer.render(new RenderRequest(
                        request.template(), request.records().get(placement.recordIndex()), request.dpi()));
                int x = pixels(placement.bounds().x(), request.dpi());
                int y = pixels(placement.bounds().y(), request.dpi());
                graphics.drawImage(label, x, y, null);
                if (request.showGuides()) {
                    graphics.setColor(new Color(37, 99, 235, 120));
                    graphics.setStroke(new BasicStroke(1));
                    graphics.drawRect(x, y, label.getWidth() - 1, label.getHeight() - 1);
                }
            }
        } finally {
            graphics.dispose();
        }
        return pageImage;
    }

    private void drawGuides(Graphics2D graphics, SheetPreviewRequest request) {
        var page = request.template().page();
        var layout = page.sheetLayout();
        graphics.setColor(new Color(148, 163, 184, 150));
        graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 4}, 0));
        for (int slot = 0; slot < layout.labelsPerPage(); slot++) {
            int row = slot / layout.columns();
            int column = slot % layout.columns();
            double xMm = page.margins().left() + column * (layout.labelSize().width() + layout.horizontalGap());
            double yMm = page.margins().top() + row * (layout.labelSize().height() + layout.verticalGap());
            graphics.drawRect(pixels(xMm, request.dpi()), pixels(yMm, request.dpi()),
                    pixels(layout.labelSize().width(), request.dpi()), pixels(layout.labelSize().height(), request.dpi()));
        }
    }

    private static int pixels(double millimetres, double dpi) {
        return Math.max(0, (int) Math.round(UnitConversions.mmToPixels(millimetres, dpi)));
    }
}

