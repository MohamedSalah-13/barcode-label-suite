package com.mohamed.barcodelabel.workflow.print;

import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.print.SheetLayoutEngine;

import java.util.List;
import java.util.Objects;

public final class UniversalPagePlanner {
    private final SheetLayoutEngine layoutEngine = new SheetLayoutEngine();

    public List<PlannedPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        Objects.requireNonNull(page, "page");
        return layoutEngine.plan(page, recordCount, startSlotIndex).stream()
                .map(sheetPage -> new PlannedPage(sheetPage.pageIndex(), sheetPage.placements().stream()
                        .map(placement -> new PlannedPlacement(placement.recordIndex(), placement.slotIndex(), placement.bounds()))
                        .toList()))
                .toList();
    }
}
