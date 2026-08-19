package com.mohamed.barcodelabel.print;

import java.util.List;

public record SheetPage(int pageIndex, List<LabelPlacement> placements) {
    public SheetPage {
        placements = List.copyOf(placements);
    }
}

