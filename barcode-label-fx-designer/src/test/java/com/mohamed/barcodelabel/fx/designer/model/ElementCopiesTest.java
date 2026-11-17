package com.mohamed.barcodelabel.fx.designer.model;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementCopiesTest {
    private static final RectMm BOUNDS = new RectMm(1, 2, 10, 5);
    private static final RectMm MOVED = new RectMm(4, 6, 12, 7);

    private final TextElement text = new TextElement(frame("Text"), "hello", "Arial", 10, "#111111",
            true, false, TextAlignment.CENTER);
    private final BarcodeElement barcode = new BarcodeElement(frame("Barcode"), "{{sku}}", Symbology.CODE_128,
            0.3, 2, true);
    private final ImageElement image = new ImageElement(frame("Image"), "logo.png", ImageFit.COVER);
    private final ShapeElement shape = new ShapeElement(frame("Shape"), ShapeType.ELLIPSE, "#000000",
            "#FFFFFF", 0.4);

    @Test
    void withBoundsKeepsEveryElementTypeAndItsContent() {
        for (LabelElement element : List.of(text, barcode, image, shape)) {
            LabelElement moved = ElementCopies.withBounds(element, MOVED);
            assertEquals(element.getClass(), moved.getClass());
            assertEquals(MOVED, moved.frame().bounds());
            assertEquals(element.frame().id(), moved.frame().id());
            assertEquals(element, ElementCopies.withBounds(moved, BOUNDS), "only the bounds changed");
        }
    }

    @Test
    void duplicateGetsANewIdACopyNameAndTheGivenLayer() {
        for (LabelElement element : List.of(text, barcode, image, shape)) {
            LabelElement copy = ElementCopies.duplicate(element, MOVED, 9);
            assertNotEquals(element.frame().id(), copy.frame().id());
            assertEquals(element.frame().name() + " copy", copy.frame().name());
            assertEquals(9, copy.frame().zIndex());
            assertEquals(MOVED, copy.frame().bounds());
        }
    }

    @Test
    void withContentChangesTextAndBarcodeDataOnly() {
        assertEquals("bye", ((TextElement) ElementCopies.withContent(text, "bye")).textTemplate());
        assertEquals("{{code}}", ((BarcodeElement) ElementCopies.withContent(barcode, "{{code}}")).dataTemplate());
        assertSame(image, ElementCopies.withContent(image, "other.png"));
        assertSame(shape, ElementCopies.withContent(shape, "ignored"));
    }

    @Test
    void frameStateChangesKeepTheRestOfTheFrame() {
        LabelElement hidden = ElementCopies.withLockedVisible(barcode, true, false);
        assertTrue(hidden.frame().locked());
        assertEquals(false, hidden.frame().visible());
        assertEquals(BOUNDS, hidden.frame().bounds());

        LabelElement rotated = ElementCopies.withRotation(shape, 90);
        assertEquals(90, rotated.frame().rotationDegrees());
        assertEquals(shape.frame().zIndex(), rotated.frame().zIndex());

        LabelElement raised = ElementCopies.withZIndex(image, 7);
        assertEquals(7, raised.frame().zIndex());
        assertEquals(image.source(), ((ImageElement) raised).source());
    }

    @Test
    void textStyleAndImageChangesIgnoreOtherElementTypes() {
        TextElement styled = (TextElement) ElementCopies.withTextStyle(text, "Tahoma", 12, "#FF0000",
                false, true, TextAlignment.RIGHT);
        assertEquals("Tahoma", styled.fontFamily());
        assertEquals("hello", styled.textTemplate());
        assertSame(barcode, ElementCopies.withTextStyle(barcode, "Tahoma", 12, "#FF0000", false, true,
                TextAlignment.RIGHT));

        ImageElement replaced = (ImageElement) ElementCopies.withImage(image, "new.png", ImageFit.STRETCH);
        assertEquals("new.png", replaced.source());
        assertEquals(ImageFit.STRETCH, replaced.fit());
        assertSame(text, ElementCopies.withImage(text, "new.png", ImageFit.STRETCH));
    }

    private static ElementFrame frame(String name) {
        return ElementFrame.create(name, BOUNDS, 3);
    }
}
