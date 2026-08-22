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
    void rotationIsUndoable() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.select(id, false);
        session.setRotation(id, 45);
        assertEquals(45, session.find(id).frame().rotationDegrees());
        session.undo();
        assertEquals(0, session.find(id).frame().rotationDegrees());
    }

    @Test
    void textStyleUpdatesOnlyTextElements() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.setTextStyle(id, "Tahoma", 14, "#FF0000", true, true, TextAlignment.RIGHT);
        var updated = (TextElement) session.find(id);
        assertEquals("Tahoma", updated.fontFamily());
        assertEquals(14, updated.fontSizePoints());
        assertEquals("#FF0000", updated.color());
        assertTrue(updated.bold());
        assertTrue(updated.italic());
        assertEquals(TextAlignment.RIGHT, updated.alignment());
    }

    @Test
    void layerOrderingMovesElementsFrontAndBack() {
        var session = new TemplateEditSession(template());
        var first = session.template().elements().get(0).frame().id();
        var second = session.template().elements().get(1).frame().id();

        session.bringToFront(first);
        assertTrue(session.find(first).frame().zIndex() > session.find(second).frame().zIndex());

        session.sendToBack(first);
        assertTrue(session.find(first).frame().zIndex() < session.find(second).frame().zIndex());

        session.bringForward(first);
        assertEquals(session.find(first).frame().zIndex() - 1, session.find(second).frame().zIndex());

        session.sendBackward(first);
        assertTrue(session.find(first).frame().zIndex() < session.find(second).frame().zIndex());
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

