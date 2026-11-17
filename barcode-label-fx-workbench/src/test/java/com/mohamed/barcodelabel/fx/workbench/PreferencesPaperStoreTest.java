package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.workflow.paper.PaperPreset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferencesPaperStoreTest {
    private Preferences node;

    @BeforeEach
    void freshNode() {
        // A node of its own under the user's preferences, removed after the test.
        node = Preferences.userRoot().node("com/mohamed/barcodelabel/test/paper-" + UUID.randomUUID());
    }

    @AfterEach
    void removeNode() throws Exception {
        node.removeNode();
        node.flush();
    }

    @Test
    void savedSizesAreReadBackInOrderInAnotherStore() {
        PaperPreset roll = new PaperPreset("custom:رول المحل", "رول المحل", "custom", new PageDefinition(PageMode.ROLL,
                new DimensionsMm(50, 30), InsetsMm.ZERO, PageOrientation.PORTRAIT, null));
        PaperPreset sheet = new PaperPreset("custom:Office", "Office", "custom", new PageDefinition(PageMode.SHEET,
                new DimensionsMm(210, 297), new InsetsMm(15.15, 4.5, 15.15, 4.5), PageOrientation.PORTRAIT,
                new SheetLayout(2, 7, new DimensionsMm(99.1, 38.1), 2.8, 0)));
        PaperPreset single = new PaperPreset("custom:One", "One", "custom", new PageDefinition(PageMode.SINGLE,
                new DimensionsMm(100, 150), new InsetsMm(1, 2, 3, 4), PageOrientation.LANDSCAPE, null));

        CustomPaperStore.preferences(node).save(List.of(roll, sheet, single));
        assertEquals(List.of(roll, sheet, single), CustomPaperStore.preferences(node).load());

        CustomPaperStore.preferences(node).save(List.of(sheet));
        assertEquals(List.of(sheet), CustomPaperStore.preferences(node).load(), "a save replaces the list");
    }

    @Test
    void aSizeThatCannotBeReadIsLeftOut() {
        PaperPreset roll = new PaperPreset("custom:Roll", "Roll", "custom", new PageDefinition(PageMode.ROLL,
                new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null));
        CustomPaperStore.preferences(node).save(List.of(roll));
        node.node("1").put("mode", "ROLL");
        node.node("1").put("width", "not a number");
        node.node("2").put("mode", "SHEET");
        assertEquals(List.of(roll), CustomPaperStore.preferences(node).load());
    }

    @Test
    void theInMemoryStoreKeepsSizesOnlyInItself() {
        CustomPaperStore store = CustomPaperStore.inMemory();
        assertEquals(List.of(), store.load());
        PaperPreset roll = new PaperPreset("custom:Roll", "Roll", "custom", new PageDefinition(PageMode.ROLL,
                new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null));
        store.save(List.of(roll));
        assertEquals(List.of(roll), store.load());
        assertEquals(List.of(), CustomPaperStore.inMemory().load());
    }
}
