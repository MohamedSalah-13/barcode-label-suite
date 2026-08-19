package com.mohamed.barcodelabel.fx.designer.model;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;

import java.util.UUID;

public final class ElementCopies {
    private ElementCopies() {}

    public static LabelElement withBounds(LabelElement element, RectMm bounds) {
        return withFrame(element, copyFrame(element.frame(), element.frame().id(), bounds,
                element.frame().name(), element.frame().zIndex()));
    }

    public static LabelElement withFrame(LabelElement element, ElementFrame frame) {
        return switch (element) {
            case TextElement value -> new TextElement(frame, value.textTemplate(), value.fontFamily(),
                    value.fontSizePoints(), value.color(), value.bold(), value.italic(), value.alignment());
            case BarcodeElement value -> new BarcodeElement(frame, value.dataTemplate(), value.symbology(),
                    value.moduleWidthMm(), value.quietZoneMm(), value.showHumanReadableText());
            case ImageElement value -> new ImageElement(frame, value.source(), value.fit());
            case ShapeElement value -> new ShapeElement(frame, value.shapeType(), value.strokeColor(),
                    value.fillColor(), value.strokeWidthMm());
        };
    }

    public static LabelElement duplicate(LabelElement element, RectMm bounds, int zIndex) {
        ElementFrame source = element.frame();
        return withFrame(element, copyFrame(source, UUID.randomUUID(), bounds, source.name() + " copy", zIndex));
    }

    public static LabelElement withContent(LabelElement element, String content) {
        return switch (element) {
            case TextElement value -> new TextElement(value.frame(), content, value.fontFamily(),
                    value.fontSizePoints(), value.color(), value.bold(), value.italic(), value.alignment());
            case BarcodeElement value -> new BarcodeElement(value.frame(), content, value.symbology(),
                    value.moduleWidthMm(), value.quietZoneMm(), value.showHumanReadableText());
            default -> element;
        };
    }

    public static LabelElement withLockedVisible(LabelElement element, boolean locked, boolean visible) {
        ElementFrame old = element.frame();
        ElementFrame frame = new ElementFrame(old.id(), old.name(), old.bounds(), old.rotationDegrees(),
                old.zIndex(), visible, locked);
        return withFrame(element, frame);
    }

    private static ElementFrame copyFrame(ElementFrame source, UUID id, RectMm bounds, String name, int zIndex) {
        return new ElementFrame(id, name, bounds, source.rotationDegrees(), zIndex,
                source.visible(), source.locked());
    }
}

