package com.mohamed.barcodelabel.fx.designer.model;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    void alignsSingleElementToLabelCenter() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.select(id, false);

        session.align(EditorAlignment.HORIZONTAL_CENTER);
        session.align(EditorAlignment.VERTICAL_CENTER);

        RectMm bounds = session.find(id).frame().bounds();
        assertEquals((63.5 - bounds.width()) / 2, bounds.x(), 0.0001);
        assertEquals((38.1 - bounds.height()) / 2, bounds.y(), 0.0001);
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
    void copyThenPasteAddsOffsetDuplicatesWithoutTouchingClipboardSource() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.select(id, false);
        session.copySelected();

        session.clearSelection();
        session.pasteClipboard();
        assertEquals(3, session.template().elements().size());
        assertEquals(1, session.selection().size());
        var pastedId = session.primarySelection();
        assertTrue(!pastedId.equals(id));
        assertEquals(7, session.find(pastedId).frame().bounds().x());
        assertEquals(8, session.find(pastedId).frame().bounds().y());

        session.pasteClipboard();
        assertEquals(4, session.template().elements().size());
    }

    @Test
    void pasteWithoutCopyIsNoOp() {
        var session = new TemplateEditSession(template());
        assertFalse(session.hasClipboard());
        session.pasteClipboard();
        assertEquals(2, session.template().elements().size());
    }

    @Test
    void selectAllSelectsEveryElement() {
        var session = new TemplateEditSession(template());
        session.selectAll();
        assertEquals(2, session.selection().size());
        session.template().elements().forEach(element -> assertTrue(session.selection().contains(element.frame().id())));
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

    @Test
    void anEditThatChangesNothingAddsNoUndoStepAndNotifiesNobody() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        var notifications = new AtomicInteger();
        session.addListener(notifications::incrementAndGet);

        session.setBounds(id, session.find(id).frame().bounds());
        session.setRotation(id, 0);
        session.setContent(id, "First");

        assertFalse(session.canUndo());
        assertEquals(0, notifications.get());
    }

    @Test
    void listenersAreNotifiedUntilRemoved() {
        var session = new TemplateEditSession(template());
        var notifications = new AtomicInteger();
        Runnable listener = notifications::incrementAndGet;
        session.addListener(listener);
        session.selectAll();
        session.removeListener(listener);
        session.clearSelection();
        assertEquals(1, notifications.get());
    }

    @Test
    void additiveSelectTogglesAnElement() {
        var session = new TemplateEditSession(template());
        var first = session.template().elements().get(0).frame().id();
        var second = session.template().elements().get(1).frame().id();
        session.select(first, false);
        session.select(second, true);
        assertEquals(Set.of(first, second), session.selection());
        session.select(first, true);
        assertEquals(Set.of(second), session.selection());
        assertEquals(second, session.primarySelection());
    }

    @Test
    void duplicateOffsetsTheCopiesAndSelectsThem() {
        var session = new TemplateEditSession(template());
        session.duplicateSelected(2);
        assertEquals(2, session.template().elements().size(), "nothing selected, nothing duplicated");

        session.selectAll();
        session.duplicateSelected(2);
        assertEquals(4, session.template().elements().size());
        assertEquals(2, session.selection().size());
        var copy = session.find(session.primarySelection());
        assertEquals(new RectMm(4, 5, 20, 6), copy.frame().bounds());
        assertEquals(2, copy.frame().zIndex(), "copies go above the existing elements");
        assertEquals("Duplicate elements", session.undoDescription());
    }

    @Test
    void lockedElementsIgnoreMoveAndResizeButNotExplicitBounds() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.setLockedVisible(id, true, true);
        session.select(id, false);

        session.moveSelected(5, 5);
        session.resize(id, new RectMm(2, 3, 40, 20));
        assertEquals(new RectMm(2, 3, 20, 6), session.find(id).frame().bounds());

        session.setBounds(id, new RectMm(10, 10, 5, 5));
        assertEquals(new RectMm(10, 10, 5, 5), session.find(id).frame().bounds());
    }

    @Test
    void boundsAreClampedInsideTheLabel() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.resize(id, new RectMm(-5, -5, 100, 100));
        assertEquals(new RectMm(0, 0, 63.5, 38.1), session.find(id).frame().bounds());
    }

    @Test
    void contentAndImageChangesApplyToTheMatchingElementTypes() {
        var image = new ImageElement(ElementFrame.create("Logo", new RectMm(1, 1, 10, 10), 2),
                "logo.png", ImageFit.CONTAIN);
        var session = new TemplateEditSession(template().withElements(List.of(text("First", new RectMm(2, 3, 20, 6), 0), image)));
        var textId = session.template().elements().getFirst().frame().id();

        session.setContent(textId, "Changed");
        assertEquals("Changed", ((TextElement) session.find(textId)).textTemplate());

        session.setImage(image.frame().id(), "other.png", ImageFit.STRETCH);
        var updated = (ImageElement) session.find(image.frame().id());
        assertEquals("other.png", updated.source());
        assertEquals(ImageFit.STRETCH, updated.fit());

        session.setContent(image.frame().id(), "ignored");
        assertEquals("other.png", ((ImageElement) session.find(image.frame().id())).source());
    }

    @Test
    void alignsSeveralElementsToTheirSharedRightAndBottomEdges() {
        var session = new TemplateEditSession(template());
        session.selectAll();
        session.align(EditorAlignment.RIGHT);
        session.align(EditorAlignment.BOTTOM);
        session.template().elements().forEach(element -> {
            var b = element.frame().bounds();
            assertEquals(50, b.x() + b.width(), 0.0001);
            assertEquals(18, b.y() + b.height(), 0.0001);
        });
        session.align(EditorAlignment.TOP);
        session.template().elements().forEach(element -> assertEquals(12, element.frame().bounds().y(), 0.0001));
    }

    @Test
    void historyDescribesTheNextStepAndEmptyHistoryIsANoOp() {
        var session = new TemplateEditSession(template());
        assertEquals("", session.undoDescription());
        assertEquals("", session.redoDescription());
        session.undo();
        session.redo();

        var id = session.template().elements().getFirst().frame().id();
        session.setRotation(id, 30);
        assertEquals("Rotate element", session.undoDescription());
        session.undo();
        assertEquals("Rotate element", session.redoDescription());
    }

    @Test
    void eachStepRecordsTheActionThePanesNameForUndo() {
        var session = new TemplateEditSession(template());
        var id = session.template().elements().getFirst().frame().id();
        session.selectAll();

        session.moveSelected(1, 1);
        assertEquals(EditAction.MOVE, session.undoAction());
        session.align(EditorAlignment.LEFT);
        assertEquals(EditAction.ALIGN, session.undoAction());
        session.setContent(id, "{{sku}}");
        assertEquals(EditAction.CHANGE_CONTENT, session.undoAction());
        session.setLockedVisible(id, false, false);
        assertEquals(EditAction.CHANGE_STATE, session.undoAction());
        session.deleteSelected();
        assertEquals(EditAction.DELETE, session.undoAction());
        session.undo();
        assertEquals(EditAction.DELETE, session.redoAction());
    }

    @Test
    void undoDropsSelectedElementsThatNoLongerExist() {
        var session = new TemplateEditSession(template());
        session.selectAll();
        session.duplicateSelected(1);
        assertEquals(2, session.selection().size());
        session.undo();
        assertTrue(session.selection().isEmpty(), "the duplicated copies are gone, so they are deselected");
    }

    @Test
    void layerCommandsIgnoreUnknownIdsAndElementsAlreadyAtTheEdge() {
        var session = new TemplateEditSession(template());
        var top = session.template().elements().get(1).frame().id();
        var bottom = session.template().elements().get(0).frame().id();
        java.util.UUID unknown = java.util.UUID.randomUUID();

        session.bringToFront(top);
        session.bringForward(top);
        session.sendToBack(bottom);
        session.sendBackward(bottom);
        session.bringToFront(unknown);
        session.sendToBack(unknown);
        session.bringForward(unknown);
        session.sendBackward(unknown);
        session.setRotation(null, 10);
        session.moveSelected(0, 0);
        assertFalse(session.canUndo());
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

