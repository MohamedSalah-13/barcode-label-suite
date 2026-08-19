package com.mohamed.barcodelabel.model.element;

import java.util.Objects;

public record ImageElement(
        ElementFrame frame,
        String source,
        ImageFit fit
) implements LabelElement {
    public ImageElement {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(fit, "fit");
    }
}

