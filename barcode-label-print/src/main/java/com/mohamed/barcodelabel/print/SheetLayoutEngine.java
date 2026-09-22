package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;

import java.util.ArrayList;
import java.util.List;

public final class SheetLayoutEngine {
    public List<SheetPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        if (recordCount < 0) {
            throw new IllegalArgumentException("recordCount must be non-negative");
        }
        if (page.mode() != PageMode.SHEET) {
            return planOneLabelPerPage(page, recordCount, startSlotIndex);
        }
        int capacity = page.sheetLayout().labelsPerPage();
        if (startSlotIndex < 0 || startSlotIndex >= capacity) {
            throw new IllegalArgumentException("startSlotIndex must be within the first page");
        }
        if (recordCount == 0) {
            return List.of();
        }

        List<SheetPage> pages = new ArrayList<>();
        int nextRecord = 0;
        int pageIndex = 0;
        while (nextRecord < recordCount) {
            int firstSlot = pageIndex == 0 ? startSlotIndex : 0;
            List<LabelPlacement> placements = new ArrayList<>();
            for (int slot = firstSlot; slot < capacity && nextRecord < recordCount; slot++) {
                placements.add(new LabelPlacement(nextRecord++, slot, boundsFor(page, slot)));
            }
            pages.add(new SheetPage(pageIndex++, placements));
        }
        return List.copyOf(pages);
    }

    private List<SheetPage> planOneLabelPerPage(PageDefinition page, int recordCount, int startSlotIndex) {
        if (startSlotIndex != 0) {
            throw new IllegalArgumentException("startSlotIndex must be zero for roll or single-label paper");
        }
        if (recordCount == 0) {
            return List.of();
        }
        var size = page.labelSize();
        var bounds = new RectMm(page.margins().left(), page.margins().top(), size.width(), size.height());
        List<SheetPage> pages = new ArrayList<>(recordCount);
        for (int index = 0; index < recordCount; index++) {
            pages.add(new SheetPage(index, List.of(new LabelPlacement(index, 0, bounds))));
        }
        return List.copyOf(pages);
    }

    private RectMm boundsFor(PageDefinition page, int slot) {
        var layout = page.sheetLayout();
        int row = slot / layout.columns();
        int column = slot % layout.columns();
        double x = page.margins().left() + column * (layout.labelSize().width() + layout.horizontalGap());
        double y = page.margins().top() + row * (layout.labelSize().height() + layout.verticalGap());
        return new RectMm(x, y, layout.labelSize().width(), layout.labelSize().height());
    }
}

