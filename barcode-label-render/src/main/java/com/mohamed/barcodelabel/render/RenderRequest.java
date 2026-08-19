package com.mohamed.barcodelabel.render;

import com.mohamed.barcodelabel.model.LabelTemplate;

import java.util.Map;
import java.util.Objects;

public record RenderRequest(LabelTemplate template, Map<String, ?> data, double dpi) {
    public RenderRequest {
        Objects.requireNonNull(template, "template");
        data = data == null ? Map.of() : Map.copyOf(data);
        if (!Double.isFinite(dpi) || dpi <= 0) {
            throw new IllegalArgumentException("dpi must be a finite positive number");
        }
    }
}

