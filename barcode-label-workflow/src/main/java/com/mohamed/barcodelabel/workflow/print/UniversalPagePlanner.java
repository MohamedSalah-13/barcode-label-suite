package com.mohamed.barcodelabel.workflow.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UniversalPagePlanner {
    public List<PlannedPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        Objects.requireNonNull(page, "page");
        if (recordCount < 0) throw new IllegalArgumentException("recordCount must be non-negative");
        if (recordCount == 0) return List.of();
        return page.mode() == PageMode.SHEET
                ? planSheet(page, recordCount, startSlotIndex)
                : planOneLabelPerPage(page, recordCount, startSlotIndex);
    }

    private List<PlannedPage> planSheet(PageDefinition page, int recordCount, int startSlotIndex) {
        int capacity = page.sheetLayout().labelsPerPage();
        if (startSlotIndex < 0 || startSlotIndex >= capacity) {
            throw new IllegalArgumentException("startSlotIndex must be within the first page");
        }
        List<PlannedPage> pages = new ArrayList<>();
        int nextRecord = 0;
        while (nextRecord < recordCount) {
            int firstSlot = pages.isEmpty() ? startSlotIndex : 0;
            List<PlannedPlacement> placements = new ArrayList<>();
            for (int slot = firstSlot; slot < capacity && nextRecord < recordCount; slot++) {
                int row = slot / page.sheetLayout().columns();
                int column = slot % page.sheetLayout().columns();
                double x = page.margins().left() + column * (page.sheetLayout().labelSize().width() + page.sheetLayout().horizontalGap());
                double y = page.margins().top() + row * (page.sheetLayout().labelSize().height() + page.sheetLayout().verticalGap());
                placements.add(new PlannedPlacement(nextRecord++, slot,
                        new RectMm(x, y, page.sheetLayout().labelSize().width(), page.sheetLayout().labelSize().height())));
            }
            pages.add(new PlannedPage(pages.size(), placements));
        }
        return List.copyOf(pages);
    }

    private List<PlannedPage> planOneLabelPerPage(PageDefinition page, int recordCount, int startSlotIndex) {
        if (startSlotIndex != 0) throw new IllegalArgumentException("startSlotIndex must be zero for roll or single-label paper");
        var size = page.labelSize();
        var bounds = new RectMm(page.margins().left(), page.margins().top(), size.width(), size.height());
        List<PlannedPage> pages = new ArrayList<>(recordCount);
        for (int index = 0; index < recordCount; index++) {
            pages.add(new PlannedPage(index, List.of(new PlannedPlacement(index, 0, bounds))));
        }
        return List.copyOf(pages);
    }
}
