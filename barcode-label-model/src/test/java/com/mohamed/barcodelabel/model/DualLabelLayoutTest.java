package com.mohamed.barcodelabel.model;

import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DualLabelLayoutTest {
    @Test void marginsAndGapLeaveEqualAreasAndCentreTracksHeight() {
        var layout = DualLabelLayout.standard();
        assertEquals(List.of(new RectMm(1, 0.5, 38, 11.5), new RectMm(1, 13, 38, 11.5)),
                layout.areas(new DimensionsMm(40, 25)));
        assertEquals(15, layout.splitAt(new DimensionsMm(40, 30)));
        var moved = new DualLabelLayout(new InsetsMm(1, 2, 3, 4), 10.0, 2, false, "#000000", false);
        assertEquals(List.of(new RectMm(4, 1, 34, 8), new RectMm(4, 11, 34, 8)),
                moved.areas(new DimensionsMm(40, 25)));
    }

    @Test void invalidAndImpossibleLayoutsAreRejected() {
        for (double bad : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new DualLabelLayout(InsetsMm.ZERO, null, bad, true, "#000000", true));
            assertThrows(IllegalArgumentException.class, () -> new DualLabelLayout(InsetsMm.ZERO, bad, 0, true, "#000000", true));
        }
        assertThrows(IllegalArgumentException.class, () -> new DualLabelLayout(InsetsMm.ZERO, null, 0, true, "red", true));
        assertThrows(IllegalArgumentException.class, () -> DualLabelLayout.standard().areas(new DimensionsMm(1, 25)));
        assertThrows(IllegalArgumentException.class, () -> DualLabelLayout.standard().areas(new DimensionsMm(40, 1)));
    }

    @Test void templateKeepsOneDesignAndPreservesLayoutAcrossEdits() {
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var plain = LabelTemplate.create("Dual", page, List.of());
        assertEquals(new DimensionsMm(40, 25), plain.designSize());
        var dual = plain.withLayout(page, DualLabelLayout.standard());
        assertEquals(2, dual.schemaVersion());
        assertEquals(new DimensionsMm(38, 11.5), dual.designSize());
        var text = new TextElement(ElementFrame.create("name", new RectMm(0, 0, 20, 3), 0), "A", "Arial", 6,
                "#000000", false, false, TextAlignment.LEFT);
        assertTrue(dual.withElements(List.of(text)).contentFits());
        assertEquals(dual.dualLayout(), dual.withElements(List.of(text)).dualLayout());
        var rotated = new TextElement(new ElementFrame(text.frame().id(), "name", text.frame().bounds(), 90, 0, true, false),
                "A", "Arial", 6, "#000000", false, false, TextAlignment.LEFT);
        assertFalse(dual.withElements(List.of(rotated)).contentFits());
        assertNull(dual.withLayout(page, null).dualLayout());
        assertThrows(IllegalArgumentException.class, () -> new LabelTemplate(1, plain.id(), plain.name(), page,
                List.of(), plain.createdAt(), plain.updatedAt(), DualLabelLayout.standard()));
    }
}
