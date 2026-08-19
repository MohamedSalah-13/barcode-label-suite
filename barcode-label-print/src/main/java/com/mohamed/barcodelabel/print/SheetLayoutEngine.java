package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;

import java.util.ArrayList;
import java.util.List;

public final class SheetLayoutEngine {
    public List<SheetPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        if (page.mode() != PageMode.SHEET) {
            throw new IllegalArgumentException("page must use SHEET mode");
        }
        if (recordCount < 0) {
            throw new IllegalArgumentException("recordCount must be non-negative");
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

    private RectMm boundsFor(PageDefinition page, int slot) {
        var layout = page.sheetLayout();
        int row = slot / layout.columns();
        int column = slot % layout.columns();
        double x = page.margins().left() + column * (layout.labelSize().width() + layout.horizontalGap());
        double y = page.margins().top() + row * (layout.labelSize().height() + layout.verticalGap());
        return new RectMm(x, y, layout.labelSize().width(), layout.labelSize().height());
    }
}

