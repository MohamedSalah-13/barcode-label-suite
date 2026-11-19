package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Function;

/** A constant-size physical layout. Pages use absolute record indices regardless of access order. */
public final class SheetPlan {
    private final PageDefinition page;
    private final int records, startSlot, capacity, pageCount;

    SheetPlan(PageDefinition page, int records, int startSlot) {
        this.page = Objects.requireNonNull(page);
        if (records < 0) throw new IllegalArgumentException("recordCount must be non-negative");
        capacity = page.mode() == PageMode.SHEET ? page.sheetLayout().labelsPerPage() : 1;
        if (startSlot < 0 || startSlot >= capacity)
            throw new IllegalArgumentException("startSlotIndex must be within the first page");
        this.records = records;
        this.startSlot = startSlot;
        pageCount = records == 0 ? 0 : (int) (((long) records + startSlot - 1) / capacity + 1);
    }

    public int pageCount() { return pageCount; }

    private int firstRecord(int pageIndex) {
        return (int) Math.clamp((long) pageIndex * capacity - startSlot, 0L, records);
    }

    /** Inclusive zero-based range, clipped to available pages. Empty ranges contain no placements. */
    public int placementCount(int from, int to) {
        int first = Math.max(0, from);
        int last = Math.min(pageCount - 1, to);
        return last < first ? 0 : firstRecord(last + 1) - firstRecord(first);
    }

    public SheetPage pageAt(int index) {
        Objects.checkIndex(index, pageCount);
        int first = firstRecord(index);
        int count = firstRecord(index + 1) - first;
        int firstSlot = index == 0 ? startSlot : 0;
        var placements = new ArrayList<LabelPlacement>(count);
        for (int i = 0; i < count; i++) {
            int slot = firstSlot + i;
            placements.add(new LabelPlacement(first + i, slot, boundsFor(slot)));
        }
        return new SheetPage(index, placements);
    }

    /** Read-only indexed view. Neither the plan nor its projections retain materialized pages. */
    public <T> List<T> pages(Function<SheetPage, T> projection) {
        Objects.requireNonNull(projection);
        class Pages extends AbstractList<T> implements RandomAccess {
            @Override public int size() { return pageCount; }
            @Override public T get(int index) { return projection.apply(pageAt(index)); }
        }
        return Collections.unmodifiableList(new Pages());
    }

    private RectMm boundsFor(int slot) {
        if (page.mode() != PageMode.SHEET) {
            var size = page.labelSize();
            return new RectMm(page.margins().left(), page.margins().top(), size.width(), size.height());
        }
        var layout = page.sheetLayout();
        int row = slot / layout.columns();
        int column = slot % layout.columns();
        double x = page.margins().left() + column * (layout.labelSize().width() + layout.horizontalGap());
        double y = page.margins().top() + row * (layout.labelSize().height() + layout.verticalGap());
        return new RectMm(x, y, layout.labelSize().width(), layout.labelSize().height());
    }
}
