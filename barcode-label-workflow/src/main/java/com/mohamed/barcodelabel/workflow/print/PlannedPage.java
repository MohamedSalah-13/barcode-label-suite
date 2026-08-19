package com.mohamed.barcodelabel.workflow.print;

import java.util.List;

public record PlannedPage(int pageIndex, List<PlannedPlacement> placements) {
    public PlannedPage {
        if (pageIndex < 0) throw new IllegalArgumentException("pageIndex must not be negative");
        placements = List.copyOf(placements);
    }
}
