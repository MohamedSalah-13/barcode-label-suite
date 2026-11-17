package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.drag;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.key;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.mouse;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignerCanvasTest {
    /** DesignerCanvas draws 4 px per mm at zoom 1. */
    private static final double PX = 4;

    private TemplateEditSession session;
    private DesignerCanvas canvas;
    private Stage stage;
    private UUID first;
    private UUID second;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @BeforeEach
    void showCanvas() {
        session = new TemplateEditSession(FxTestSupport.twoTextTemplate());
        first = session.template().elements().get(0).frame().id();
        second = session.template().elements().get(1).frame().id();
        runFx(() -> {
            canvas = new DesignerCanvas(session);
            stage = new Stage();
            stage.setScene(new Scene(new StackPane(canvas), 400, 300));
            stage.show();
            canvas.applyCss();
            canvas.layout();
        });
    }

    @AfterEach
    void closeWindows() {
        runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void drawsVisibleElementsAtTheLabelsSizeAndHidesInvisibleOnes() {
        runFx(() -> {
            assertEquals(FxTestSupport.LABEL_WIDTH * PX, canvas.getPrefWidth(), 0.001);
            assertEquals(2, elementViews().size());
            session.setLockedVisible(second, false, false);
            assertEquals(1, elementViews().size());
        });
    }

    @Test
    void zoomScalesTheCanvasWithinItsLimits() {
        runFx(() -> {
            canvas.setZoom(2);
            assertEquals(FxTestSupport.LABEL_WIDTH * PX * 2, canvas.getPrefWidth(), 0.001);
            canvas.setZoom(100);
            assertEquals(PX * 4, canvas.pixelsPerMm(), 0.001);
            canvas.setZoom(0.01);
            assertEquals(PX * 0.5, canvas.pixelsPerMm(), 0.001);
            canvas.setGridMm(0);
            canvas.setGridMm(5);
        });
    }

    @Test
    void arrowKeysNudgeTheSelectionByATenthOrOneMillimetre() {
        runFx(() -> {
            session.select(first, false);
            key(canvas, KeyCode.RIGHT, false, false);
            assertEquals(2.1, x(first), 0.0001);
            key(canvas, KeyCode.RIGHT, false, true);
            assertEquals(3.1, x(first), 0.0001);
            key(canvas, KeyCode.LEFT, false, true);
            key(canvas, KeyCode.DOWN, false, true);
            assertEquals(2.1, x(first), 0.0001);
            assertEquals(4, y(first), 0.0001);
            key(canvas, KeyCode.UP, false, false);
            assertEquals(3.9, y(first), 0.0001);
            key(canvas, KeyCode.Q, false, false);
            assertEquals(3.9, y(first), 0.0001);
        });
    }

    @Test
    void editingShortcutsDuplicateCopyPasteCutDeleteUndoAndRedo() {
        runFx(() -> {
            session.select(first, false);
            key(canvas, KeyCode.D, true, false);
            assertEquals(3, count());

            key(canvas, KeyCode.Z, true, false);
            assertEquals(2, count());
            key(canvas, KeyCode.Y, true, false);
            assertEquals(3, count());
            key(canvas, KeyCode.Z, true, false);
            key(canvas, KeyCode.Z, true, true);
            assertEquals(3, count(), "Ctrl+Shift+Z redoes");

            key(canvas, KeyCode.A, true, false);
            assertEquals(3, session.selection().size());
            key(canvas, KeyCode.C, true, false);
            key(canvas, KeyCode.V, true, false);
            assertEquals(6, count());

            key(canvas, KeyCode.X, true, false);
            assertEquals(3, count());
            assertTrue(session.hasClipboard());

            session.select(first, false);
            key(canvas, KeyCode.DELETE, false, false);
            assertEquals(2, count());
            session.selectAll();
            key(canvas, KeyCode.BACK_SPACE, false, false);
            assertEquals(0, count());
        });
    }

    @Test
    void draggingAnElementMovesItByTheDraggedDistanceAsOneUndoStep() {
        runFx(() -> {
            drag(view(0), 10, 10, 10 * PX, 0);
            assertEquals(12, x(first), 0.0001);
            assertEquals(3, y(first), 0.0001);
            assertEquals(Set.of(first), session.selection());
            session.undo();
            assertEquals(2, x(first), 0.0001);
        });
    }

    @Test
    void dragMovementSnapsToTheGrid() {
        runFx(() -> {
            drag(view(0), 10, 10, 10.4 * PX, 0);
            assertEquals(12, x(first), 0.0001, "10.4 mm snaps to the 1 mm grid");
        });
    }

    @Test
    void draggingCloseToAnotherElementsEdgeSnapsToItAndShowsAGuide() {
        runFx(() -> {
            canvas.setSnapEnabled(false);
            Node element = view(0);
            mouse(element, MouseEvent.MOUSE_PRESSED, 10, 10, false);
            // 27 mm moves the left edge to 29 mm, within the 1.5 mm tolerance of the other element at 30 mm.
            mouse(element, MouseEvent.MOUSE_DRAGGED, 10 + 27 * PX, 10, false);
            assertTrue(guideVisible(), "a smart guide is shown while snapping");
            mouse(element, MouseEvent.MOUSE_RELEASED, 10 + 27 * PX, 10, false);

            assertEquals(30, x(first), 0.0001);
            assertFalse(guideVisible(), "guides are hidden after the drag");
        });
    }

    @Test
    void lockedElementsCannotBeMovedOrResized() {
        runFx(() -> {
            session.setLockedVisible(first, true, true);
            drag(view(0), 10, 10, 10 * PX, 10 * PX);
            drag(handle(view(0)), 80, 24, 10 * PX, 10 * PX);
            assertEquals(new RectMm(2, 3, 20, 6), session.find(first).frame().bounds());
        });
    }

    @Test
    void theResizeHandleChangesWidthAndHeight() {
        runFx(() -> {
            drag(handle(view(0)), 80, 24, 10 * PX, 5 * PX);
            RectMm bounds = session.find(first).frame().bounds();
            assertEquals(30, bounds.width(), 0.0001);
            assertEquals(11, bounds.height(), 0.0001);
            assertEquals(2, bounds.x(), 0.0001);
        });
    }

    @Test
    void ctrlClickAddsToTheSelectionAndClickingEmptySpaceClearsIt() {
        runFx(() -> {
            mouse(view(0), MouseEvent.MOUSE_PRESSED, 10, 10, false);
            mouse(view(1), MouseEvent.MOUSE_PRESSED, 130, 50, true);
            assertEquals(Set.of(first, second), session.selection());

            band(60 * PX, 35 * PX, 60 * PX + 1, 35 * PX + 1, false);
            assertTrue(session.selection().isEmpty());
        });
    }

    @Test
    void elementViewsTakeTheirElementsSize() {
        runFx(() -> {
            // The views are unmanaged, so the canvas must size them itself; at 0 x 0 their content is invisible.
            assertEquals(20 * PX, view(0).getWidth(), 0.001);
            assertEquals(6 * PX, view(0).getHeight(), 0.001);
        });
    }

    @Test
    void rubberBandSelectsTheElementsItTouches() {
        runFx(() -> {
            // Over the first element's top-left part only (it spans 2..22 x 3..9 mm).
            band(1, 1, 5 * PX, 5 * PX, false);
            assertEquals(Set.of(first), session.selection());

            // Over the middle of the second element (30..50 x 12..18 mm), away from its top-left corner;
            // Ctrl keeps the first element selected.
            band(60 * PX, 30 * PX, 45 * PX, 15 * PX, true);
            assertEquals(Set.of(first, second), session.selection());

            // Without Ctrl, a band over empty space clears the selection.
            band(60 * PX, 35 * PX, 55 * PX, 25 * PX, false);
            assertTrue(session.selection().isEmpty());
        });
    }

    @Test
    void elementContextMenuRunsEditingAndLayerCommands() {
        runFx(() -> {
            openContextMenu(view(0));
            assertEquals(Set.of(first), session.selection(), "opening the menu selects the element");
            assertTrue(item("لصق").isDisable(), "paste is disabled with an empty clipboard");
            item("تكرار").fire();
            assertEquals(3, count());

            session.select(first, false);
            openContextMenu(view(0));
            item("إحضار للمقدمة").fire();
            assertEquals(maxZ(), session.find(first).frame().zIndex());
            openContextMenu(view(viewIndexOf(first)));
            item("إرسال للخلفية").fire();
            assertEquals(minZ(), session.find(first).frame().zIndex());
            openContextMenu(view(viewIndexOf(first)));
            item("طبقة للأمام").fire();
            openContextMenu(view(viewIndexOf(first)));
            item("طبقة للخلف").fire();
            assertEquals(minZ(), session.find(first).frame().zIndex());

            openContextMenu(view(viewIndexOf(first)));
            item("نسخ").fire();
            openContextMenu(view(viewIndexOf(first)));
            assertFalse(item("لصق").isDisable());
            item("لصق").fire();
            assertEquals(4, count());

            session.select(first, false);
            openContextMenu(view(viewIndexOf(first)));
            item("قص").fire();
            assertEquals(3, count());
            assertNull(session.find(first));

            session.select(second, false);
            openContextMenu(view(viewIndexOf(second)));
            item("حذف").fire();
            assertEquals(2, count());
        });
    }

    @Test
    void canvasContextMenuPastesTheClipboard() {
        runFx(() -> {
            openContextMenu(canvas);
            assertTrue(item("لصق").isDisable());

            session.select(first, false);
            session.copySelected();
            openContextMenu(canvas);
            item("لصق").fire();
            assertEquals(3, count());
        });
    }

    /** Drags a selection band on the canvas between two points in canvas coordinates. */
    private void band(double fromX, double fromY, double toX, double toY, boolean control) {
        var from = canvas.localToScene(fromX, fromY);
        var to = canvas.localToScene(toX, toY);
        mouse(canvas, MouseEvent.MOUSE_PRESSED, from.getX(), from.getY(), control);
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, to.getX(), to.getY(), control);
        mouse(canvas, MouseEvent.MOUSE_RELEASED, to.getX(), to.getY(), control);
    }

    private List<StackPane> elementViews() {
        return canvas.getChildren().stream()
                .filter(node -> node.getStyleClass().contains("designer-element"))
                .map(StackPane.class::cast)
                .toList();
    }

    /** Element views are added in z order, so the index of an element's view follows its z index. */
    private StackPane view(int index) {
        return elementViews().get(index);
    }

    private int viewIndexOf(UUID id) {
        var ordered = session.template().elements().stream()
                .sorted((a, b) -> Integer.compare(a.frame().zIndex(), b.frame().zIndex()))
                .map(element -> element.frame().id())
                .toList();
        return ordered.indexOf(id);
    }

    private static Node handle(StackPane view) {
        return view.lookup(".resize-handle");
    }

    private boolean guideVisible() {
        return canvas.getChildren().stream()
                .filter(node -> node instanceof Line && node.getStyleClass().contains("smart-guide"))
                .anyMatch(Node::isVisible);
    }

    private static void openContextMenu(Node target) {
        Event.fireEvent(target, new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                10, 10, 10, 10, false, null));
    }

    private static MenuItem item(String text) {
        ContextMenu menu = Window.getWindows().stream()
                .filter(window -> window instanceof ContextMenu && window.isShowing())
                .map(ContextMenu.class::cast)
                .reduce((older, newer) -> newer)
                .orElse(null);
        assertNotNull(menu, "a context menu is showing");
        return menu.getItems().stream()
                .filter(item -> text.equals(item.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no menu item " + text));
    }

    private double x(UUID id) { return session.find(id).frame().bounds().x(); }
    private double y(UUID id) { return session.find(id).frame().bounds().y(); }
    private int count() { return session.template().elements().size(); }

    private int maxZ() {
        return session.template().elements().stream().mapToInt(e -> e.frame().zIndex()).max().orElseThrow();
    }

    private int minZ() {
        return session.template().elements().stream().mapToInt(e -> e.frame().zIndex()).min().orElseThrow();
    }
}
