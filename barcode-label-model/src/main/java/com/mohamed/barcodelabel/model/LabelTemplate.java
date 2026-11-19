package com.mohamed.barcodelabel.model;

import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.DualLabelLayout;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;

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
        Instant updatedAt,
        DualLabelLayout dualLayout
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public LabelTemplate(int schemaVersion, UUID id, String name, PageDefinition page,
                         List<LabelElement> elements, Instant createdAt, Instant updatedAt) {
        this(schemaVersion, id, name, page, elements, createdAt, updatedAt, null);
    }

    public LabelTemplate {
        if (schemaVersion <= 0 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schemaVersion: " + schemaVersion);
        }
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(page, "page");
        if (dualLayout != null) {
            if (schemaVersion < 2) throw new IllegalArgumentException("dual layout requires schemaVersion 2");
            dualLayout.areas(page.labelSize());
        }
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
        // Single labels retain the original format for compatibility with existing consumers.
        return new LabelTemplate(1, UUID.randomUUID(), name, page, elements, now, now);
    }

    public LabelTemplate withElements(List<LabelElement> newElements) {
        return new LabelTemplate(schemaVersion, id, name, page, newElements, createdAt, Instant.now(), dualLayout);
    }

    public LabelTemplate withLayout(PageDefinition newPage,
            DualLabelLayout layout) {
        return new LabelTemplate(layout == null ? schemaVersion : 2, id, name, newPage, elements,
                createdAt, Instant.now(), layout);
    }

    /** Elements are local to the shared content area, not to the second copy. */
    public DimensionsMm designSize() {
        var area = contentAreas().getFirst();
        return new DimensionsMm(area.width(), area.height());
    }

    public List<RectMm> contentAreas() {
        var size = page.labelSize();
        return dualLayout == null
                ? List.of(new RectMm(0, 0, size.width(), size.height()))
                : dualLayout.areas(size);
    }

    public boolean contentFits() {
        var size = designSize();
        return elements.stream().filter(e -> e.frame().visible()).allMatch(e -> {
            var b = e.frame().bounds();
            double angle = Math.toRadians(e.frame().rotationDegrees());
            double w = Math.abs(b.width() * Math.cos(angle)) + Math.abs(b.height() * Math.sin(angle));
            double h = Math.abs(b.width() * Math.sin(angle)) + Math.abs(b.height() * Math.cos(angle));
            double x = b.x() + (b.width() - w) / 2, y = b.y() + (b.height() - h) / 2;
            return x >= -0.001 && y >= -0.001 && x + w <= size.width() + 0.001 && y + h <= size.height() + 0.001;
        });
    }
}

