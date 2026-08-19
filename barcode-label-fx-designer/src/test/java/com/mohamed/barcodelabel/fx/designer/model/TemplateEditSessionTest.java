package com.mohamed.barcodelabel.fx.designer.model;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateEditSessionTest {
    @Test
    void moveIsOneUndoableOperation() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.select(id, false);
        session.moveSelected(5, 3);

        assertEquals(7, session.find(id).frame().bounds().x());
        assertTrue(session.canUndo());
        session.undo();
        assertEquals(2, session.find(id).frame().bounds().x());
        session.redo();
        assertEquals(7, session.find(id).frame().bounds().x());
    }

    @Test
    void alignsMultipleElementsAndKeepsThemInsideLabel() {
        var session = new TemplateEditSession(template());
        session.template().elements().forEach(element -> session.select(element.frame().id(), true));
        session.align(EditorAlignment.LEFT);
        assertEquals(2, session.template().elements().get(0).frame().bounds().x());
        assertEquals(2, session.template().elements().get(1).frame().bounds().x());

        session.moveSelected(1000, 1000);
        session.template().elements().forEach(element -> {
            var b = element.frame().bounds();
            assertTrue(b.x() + b.width() <= 63.5);
            assertTrue(b.y() + b.height() <= 38.1);
        });
    }

    @Test
    void deleteAndUndoRestoreSelectionTargets() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.select(id, false);
        session.deleteSelected();
        assertEquals(1, session.template().elements().size());
        assertFalse(session.canRedo());
        session.undo();
        assertEquals(2, session.template().elements().size());
    }

    private static LabelTemplate template() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var first = text("First", new RectMm(2, 3, 20, 6), 0);
        var second = text("Second", new RectMm(30, 12, 20, 6), 1);
        return LabelTemplate.create("Editor test", page, List.of(first, second));
    }

    private static TextElement text(String value, RectMm bounds, int z) {
        return new TextElement(ElementFrame.create(value, bounds, z), value, "Arial", 10,
                "#111827", false, false, TextAlignment.LEFT);
    }
}

