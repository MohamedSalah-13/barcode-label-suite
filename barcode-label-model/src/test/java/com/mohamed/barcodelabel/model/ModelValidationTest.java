package com.mohamed.barcodelabel.model;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The value checks in the model's records: what each accepts at its boundary and what it rejects. */
class ModelValidationTest {
    private static final double[] NOT_FINITE = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    private static final RectMm BOUNDS = new RectMm(0, 0, 10, 5);

    @Test
    void dimensionsMustBePositive() {
        rejects("width must be a finite positive number", () -> new DimensionsMm(0, 1));
        rejects("height must be a finite positive number", () -> new DimensionsMm(1, -1));
        for (double bad : NOT_FINITE) rejects("width must be a finite positive number", () -> new DimensionsMm(bad, 1));
    }

    @Test
    void rectanglesMayStartAnywhereButNeedASize() {
        assertDoesNotThrow(() -> new RectMm(-5, -5, 1, 1));
        rejects("width must be a finite positive number", () -> new RectMm(0, 0, 0, 1));
        rejects("height must be a finite positive number", () -> new RectMm(0, 0, 1, -2));
        for (double bad : NOT_FINITE) {
            rejects("x must be finite", () -> new RectMm(bad, 0, 1, 1));
            rejects("y must be finite", () -> new RectMm(0, bad, 1, 1));
        }
    }

    @Test
    void insetsMayBeZeroButNotNegative() {
        assertDoesNotThrow(() -> new InsetsMm(0, 0, 0, 0));
        rejects("top must be finite and non-negative", () -> new InsetsMm(-1, 0, 0, 0));
        rejects("left must be finite and non-negative", () -> new InsetsMm(0, 0, 0, Double.NaN));
    }

    @Test
    void sheetLayoutsNeedRowsAndColumnsButMayHaveNoGaps() {
        DimensionsMm label = new DimensionsMm(60, 30);
        assertDoesNotThrow(() -> new SheetLayout(1, 1, label, 0, 0));
        rejects("columns must be positive", () -> new SheetLayout(0, 1, label, 0, 0));
        rejects("rows must be positive", () -> new SheetLayout(1, -3, label, 0, 0));
        rejects("horizontalGap must be finite and non-negative", () -> new SheetLayout(1, 1, label, -0.1, 0));
        rejects("verticalGap must be finite and non-negative", () -> new SheetLayout(1, 1, label, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    void elementFramesNeedAFiniteRotation() {
        assertDoesNotThrow(() -> new ElementFrame(UUID.randomUUID(), "a", BOUNDS, -90, 0, true, false));
        for (double bad : NOT_FINITE) {
            rejects("rotationDegrees must be finite", () -> new ElementFrame(UUID.randomUUID(), "a", BOUNDS, bad, 0, true, false));
        }
    }

    @Test
    void barcodesNeedAModuleWidthButMayHaveNoQuietZone() {
        assertDoesNotThrow(() -> barcode(0.25, 0));
        rejects("moduleWidthMm must be a finite positive number", () -> barcode(0, 2));
        rejects("quietZoneMm must be finite and non-negative", () -> barcode(0.25, -1));
        rejects("quietZoneMm must be finite and non-negative", () -> barcode(0.25, Double.NaN));
    }

    @Test
    void textNeedsAFontSizeAndShapesMayHaveNoStroke() {
        rejects("fontSizePoints must be a finite positive number", () -> new TextElement(frame(), "x", "Arial", 0,
                "#000000", false, false, TextAlignment.LEFT));
        assertDoesNotThrow(() -> new ShapeElement(frame(), ShapeType.LINE, "#000000", "transparent", 0));
        rejects("strokeWidthMm must be finite and non-negative", () -> new ShapeElement(frame(), ShapeType.LINE,
                "#000000", "transparent", -0.5));
    }

    @Test
    void unitConversionsNeedAPositiveDpi() {
        assertEquals(300, UnitConversions.mmToPixels(25.4, 300), 1e-9);
        rejects("dpi must be a finite positive number", () -> UnitConversions.mmToPixels(1, 0));
        rejects("dpi must be a finite positive number", () -> UnitConversions.pixelsToMm(1, Double.NaN));
    }

    private static void rejects(String message, Executable construction) {
        assertEquals(message, assertThrows(IllegalArgumentException.class, construction).getMessage());
    }

    private static BarcodeElement barcode(double moduleWidth, double quietZone) {
        return new BarcodeElement(frame(), "123", Symbology.CODE_128, moduleWidth, quietZone, true);
    }

    private static ElementFrame frame() {
        return ElementFrame.create("element", BOUNDS, 0);
    }
}
