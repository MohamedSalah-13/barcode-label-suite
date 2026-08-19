package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SheetLayoutEngineTest {
    @Test
    void startsAtRequestedSlotAndContinuesOnNextPage() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));

        var pages = new SheetLayoutEngine().plan(page, 20, 5);

        assertEquals(2, pages.size());
        assertEquals(5, pages.getFirst().placements().getFirst().slotIndex());
        assertEquals(16, pages.getFirst().placements().size());
        assertEquals(4, pages.get(1).placements().size());
        assertEquals(139.25, pages.getFirst().placements().getFirst().bounds().x(), 0.001);
        assertEquals(53.25, pages.getFirst().placements().getFirst().bounds().y(), 0.001);
    }
}
