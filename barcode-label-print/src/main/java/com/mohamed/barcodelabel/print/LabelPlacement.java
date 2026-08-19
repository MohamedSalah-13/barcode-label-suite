package com.mohamed.barcodelabel.print;

import com.mohamed.barcodelabel.model.geometry.RectMm;

public record LabelPlacement(int recordIndex, int slotIndex, RectMm bounds) {
}

