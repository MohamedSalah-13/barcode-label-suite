package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SheetPageRendererTest {
    @Test
    void rendersRecordsFromRequestedStartSlot() throws Exception {
        var request = new SheetPreviewRequest(template(), records(20), 5, 0, 96, false);
        var renderer = new SheetPageRenderer();
        BufferedImage page = renderer.render(request);

        assertEquals(794, page.getWidth());
        assertEquals(1123, page.getHeight());
        assertEquals(Color.WHITE.getRGB(), page.getRGB(40, 70));
        assertEquals(Color.BLACK.getRGB(), page.getRGB(545, 220));
        assertEquals(2, renderer.pageCount(request));
    }

    @Test
    void printableReportsExistingAndMissingPagesWithoutARealPrinter() throws Exception {
        var printRequest = new LabelPrintRequest(template(), records(20), "Test printer", 1, 5);
        var printable = new SheetPrintable(printRequest, 150);
        BufferedImage surface = new BufferedImage(700, 900, BufferedImage.TYPE_INT_RGB);

        assertEquals(java.awt.print.Printable.PAGE_EXISTS,
                printable.print(surface.createGraphics(), new PageFormat(), 0));
        assertEquals(java.awt.print.Printable.NO_SUCH_PAGE,
                printable.print(surface.createGraphics(), new PageFormat(), 2));
    }

    private static LabelTemplate template() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var marker = new ShapeElement(
                ElementFrame.create("Marker", new RectMm(1, 1, 10, 10), 0),
                ShapeType.RECTANGLE, "#000000", "#000000", 0.1);
        return LabelTemplate.create("Test sheet", page, List.of(marker));
    }

    private static List<Map<String, ?>> records(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> Map.<String, Object>of("index", index))
                .toList();
    }
}

