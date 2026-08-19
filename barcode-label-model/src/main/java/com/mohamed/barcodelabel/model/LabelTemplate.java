package com.mohamed.barcodelabel.model;

import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.page.PageDefinition;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LabelTemplate(
        int schemaVersion,
        UUID id,
        String name,
        PageDefinition page,
        List<LabelElement> elements,
        Instant createdAt,
        Instant updatedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public LabelTemplate {
        if (schemaVersion <= 0 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schemaVersion: " + schemaVersion);
        }
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        elements = List.copyOf(elements);
        var ids = new HashSet<UUID>();
        for (LabelElement element : elements) {
            Objects.requireNonNull(element, "elements must not contain null");
            if (!ids.add(element.frame().id())) {
                throw new IllegalArgumentException("duplicate element id: " + element.frame().id());
            }
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static LabelTemplate create(String name, PageDefinition page, List<LabelElement> elements) {
        Instant now = Instant.now();
        return new LabelTemplate(CURRENT_SCHEMA_VERSION, UUID.randomUUID(), name, page, elements, now, now);
    }

    public LabelTemplate withElements(List<LabelElement> newElements) {
        return new LabelTemplate(schemaVersion, id, name, page, newElements, createdAt, Instant.now());
    }
}

