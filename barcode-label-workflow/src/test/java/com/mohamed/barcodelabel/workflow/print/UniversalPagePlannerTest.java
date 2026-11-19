package com.mohamed.barcodelabel.workflow.print;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UniversalPagePlannerTest {
    private final UniversalPagePlanner planner = new UniversalPagePlanner();

    @Test void singleLabelPagesSupportDirectLastPageAccess() {
        var paper = new PageDefinition(PageMode.SINGLE, new DimensionsMm(50, 30), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var pages = planner.plan(paper, Integer.MAX_VALUE, 0);
        assertEquals(Integer.MAX_VALUE, pages.size());
        assertEquals(Integer.MAX_VALUE - 1, pages.getLast().placements().getFirst().recordIndex());
        assertEquals(0, pages.getFirst().placements().getFirst().slotIndex());
        assertThrows(IndexOutOfBoundsException.class, () -> pages.get(-1));
    }

    @Test void sheetHonoursFirstEmptySlots() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var pages = planner.plan(page, 22, 2);
        assertEquals(2, pages.size());
        assertEquals(19, pages.getFirst().placements().size());
        assertEquals(2, pages.getFirst().placements().getFirst().slotIndex());
    }

    @Test void rollUsesOnePhysicalLabelPerPage() {
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(50, 30), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var pages = planner.plan(page, 3, 0);
        assertEquals(3, pages.size());
        assertEquals(50, pages.getFirst().placements().getFirst().bounds().width());
    }
}
