package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabelPrintRequestTest {
    @Test
    void fiveArgConstructorDefaultsToUnrestrictedPageRange() {
        var request = new LabelPrintRequest(template(), records(), "Printer", 1, 0);
        assertEquals(0, request.firstPage());
        assertEquals(0, request.lastPage());
    }

    @Test
    void rejectsNegativePageBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> new LabelPrintRequest(template(), records(), "Printer", 1, 0, -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LabelPrintRequest(template(), records(), "Printer", 1, 0, 0, -1));
    }

    @Test
    void rejectsFirstPageAfterLastPage() {
        assertThrows(IllegalArgumentException.class,
                () -> new LabelPrintRequest(template(), records(), "Printer", 1, 0, 5, 2));
    }

    @Test
    void acceptsAValidPageRange() {
        var request = new LabelPrintRequest(template(), records(), "Printer", 1, 0, 2, 5);
        assertEquals(2, request.firstPage());
        assertEquals(5, request.lastPage());
    }

    private static LabelTemplate template() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var marker = new ShapeElement(ElementFrame.create("Marker", new RectMm(1, 1, 10, 10), 0),
                ShapeType.RECTANGLE, "#000000", "#000000", 0.1);
        return LabelTemplate.create("Test sheet", page, List.of(marker));
    }

    private static List<Map<String, ?>> records() {
        return List.of(Map.of("index", 0));
    }
}
