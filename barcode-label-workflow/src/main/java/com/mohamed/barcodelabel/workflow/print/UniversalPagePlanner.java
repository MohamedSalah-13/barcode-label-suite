package com.mohamed.barcodelabel.workflow.print;

import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.print.SheetLayoutEngine;

import java.util.List;
import com.mohamed.barcodelabel.print.SheetPlan;

public final class UniversalPagePlanner {
    private final SheetLayoutEngine layoutEngine = new SheetLayoutEngine();

    public List<PlannedPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        return pages(layout(page, recordCount, startSlotIndex));
    }

    public SheetPlan layout(PageDefinition page, int recordCount, int startSlotIndex) {
        return layoutEngine.layout(page, recordCount, startSlotIndex);
    }

    public List<PlannedPage> pages(SheetPlan layout) {
        return layout.pages(sheetPage -> new PlannedPage(sheetPage.pageIndex(), sheetPage.placements().stream()
                .map(placement -> new PlannedPlacement(placement.recordIndex(), placement.slotIndex(), placement.bounds()))
                .toList()));
    }
}
