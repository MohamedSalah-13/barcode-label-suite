package com.mohamed.barcodelabel.workflow.paper;

import com.mohamed.barcodelabel.model.page.PageDefinition;

import java.util.Objects;

public record PaperPreset(String id, String displayName, String category, PageDefinition page) {
    public PaperPreset {
        requireText(id, "id");
        requireText(displayName, "displayName");
        requireText(category, "category");
        Objects.requireNonNull(page, "page");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }

    @Override public String toString() { return displayName; }
}
