package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.page.PageDefinition;
import java.util.List;
import java.util.function.Function;

public final class SheetLayoutEngine {
    public SheetPlan layout(PageDefinition page, int recordCount, int startSlotIndex) {
        return new SheetPlan(page, recordCount, startSlotIndex);
    }

    public List<SheetPage> plan(PageDefinition page, int recordCount, int startSlotIndex) {
        return layout(page, recordCount, startSlotIndex).pages(Function.identity());
    }
}
