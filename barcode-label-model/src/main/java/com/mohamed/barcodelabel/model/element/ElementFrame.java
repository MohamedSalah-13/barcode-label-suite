package com.mohamed.barcodelabel.model.element;

import com.mohamed.barcodelabel.model.geometry.RectMm;

import java.util.Objects;
import java.util.UUID;

import static com.mohamed.barcodelabel.model.internal.Checks.requireFinite;

public record ElementFrame(
        UUID id,
        String name,
        RectMm bounds,
        double rotationDegrees,
        int zIndex,
        boolean visible,
        boolean locked
) {
    public ElementFrame {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        requireFinite(rotationDegrees, "rotationDegrees");
    }

    public static ElementFrame create(String name, RectMm bounds, int zIndex) {
        return new ElementFrame(UUID.randomUUID(), name, bounds, 0, zIndex, true, false);
    }
}

