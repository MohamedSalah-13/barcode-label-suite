package com.mohamed.barcodelabel.model.element;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextElement.class, name = "text"),
        @JsonSubTypes.Type(value = BarcodeElement.class, name = "barcode"),
        @JsonSubTypes.Type(value = ImageElement.class, name = "image"),
        @JsonSubTypes.Type(value = ShapeElement.class, name = "shape")
})
public sealed interface LabelElement permits TextElement, BarcodeElement, ImageElement, ShapeElement {
    ElementFrame frame();
}

