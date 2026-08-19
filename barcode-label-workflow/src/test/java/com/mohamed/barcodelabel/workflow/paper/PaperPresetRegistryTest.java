package com.mohamed.barcodelabel.workflow.paper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaperPresetRegistryTest {
    @Test void builtInPresetsAreValidAndUnique() {
        PaperPresetRegistry registry = new PaperPresetRegistry();
        assertEquals(7, registry.all().size());
        assertTrue(registry.find("a4-3x7-63.5x38.1").isPresent());
        assertEquals(21, registry.find("a4-3x7-63.5x38.1").orElseThrow().page().sheetLayout().labelsPerPage());
    }

    @Test void duplicateRegistrationIsRejected() {
        PaperPresetRegistry registry = new PaperPresetRegistry();
        PaperPreset first = registry.all().getFirst();
        assertThrows(IllegalArgumentException.class, () -> registry.register(first));
    }
}
