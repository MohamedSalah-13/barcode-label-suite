package com.mohamed.barcodelabel.model.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitConversionsTest {
    @Test
    void convertsMillimetresToPoints() {
        assertEquals(72.0, UnitConversions.mmToPoints(25.4), 0.000001);
    }

    @Test
    void roundTripsPixelsAtPrinterDpi() {
        double pixels = UnitConversions.mmToPixels(50, 300);
        assertEquals(50, UnitConversions.pixelsToMm(pixels, 300), 0.000001);
    }

    @Test
    void rejectsInvalidDpi() {
        assertThrows(IllegalArgumentException.class, () -> UnitConversions.mmToPixels(10, 0));
    }
}

