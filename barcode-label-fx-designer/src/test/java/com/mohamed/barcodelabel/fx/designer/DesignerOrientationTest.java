package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
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

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.drag;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.key;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.mouse;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The designer UI is right to left for Arabic, but the label surface must keep the label's own left-to-right
 * millimetre coordinates, so elements appear, move and resize where they print. Everything here is measured in
 * scene coordinates, which is what the user sees on screen.
 */
class DesignerOrientationTest {
    /** DesignerCanvas draws 4 px per mm at zoom 1. */
    private static final double PX = 4;

    private LabelDesignerPane pane;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @BeforeEach
    void showDesigner() {
        runFx(() -> {
            pane = new LabelDesignerPane(FxTestSupport.twoTextTemplate());
            Stage stage = new Stage();
            stage.setScene(new Scene(pane, 1200, 640));
            stage.show();
            pane.applyCss();
            pane.layout();
        });
    }

    @AfterEach
    void closeWindows() {
        runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void theDesignerIsRightToLeftButTheLabelSurfaceIsNot() {
        runFx(() -> {
            assertEquals(NodeOrientation.RIGHT_TO_LEFT, pane.getEffectiveNodeOrientation());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, canvas().getEffectiveNodeOrientation());
        });
    }

    @Test
    void theLabelSitsAtTheRightOfTheWorkspaceLikeTheRestOfTheRightToLeftUi() {
        runFx(() -> {
            Bounds workspace = sceneBounds(pane.getCenter());
            Bounds label = sceneBounds(canvas());
            assertTrue(label.getMinX() - workspace.getMinX() > workspace.getMaxX() - label.getMaxX(),
                    "the label is closer to the workspace's right edge: " + label + " in " + workspace);
            // The workspace's 32 px padding, give or take its border.
            assertEquals(32, workspace.getMaxX() - label.getMaxX(), 3);
            assertEquals(32, label.getMinY() - workspace.getMinY(), 3);

            canvas().setZoom(2);
            pane.layout();
            Bounds zoomed = sceneBounds(canvas());
            assertEquals(label.getWidth() * 2, zoomed.getWidth(), 0.5);
            assertEquals(32, workspace.getMaxX() - zoomed.getMaxX(), 3, "it stays on the right when zoomed");
        });
    }

    @Test
    void elementsAppearAtTheirDistanceFromTheLabelsLeftEdge() {
        runFx(() -> {
            double labelLeft = sceneBounds(canvas()).getMinX();
            // "First" starts 2 mm and "Second" 30 mm from the label's left edge, as they print.
            assertEquals(2 * PX, sceneBounds(view(0)).getMinX() - labelLeft, 0.5);
            assertEquals(30 * PX, sceneBounds(view(1)).getMinX() - labelLeft, 0.5);
        });
    }

    @Test
    void draggingRightMovesTheElementRightOnScreen() {
        runFx(() -> {
            double before = sceneBounds(view(0)).getMinX();
            drag(view(0), 500, 100, 10 * PX, 0);
            assertEquals(12, pane.getTemplate().elements().getFirst().frame().bounds().x(), 0.0001);
            assertEquals(before + 10 * PX, sceneBounds(view(0)).getMinX(), 0.5);
        });
    }

    @Test
    void theRightArrowKeyMovesTheElementRightOnScreen() {
        runFx(() -> {
            drag(view(0), 500, 100, 0, 0); // select it
            double before = sceneBounds(view(0)).getMinX();
            key(canvas(), KeyCode.RIGHT, false, true);
            assertEquals(3, pane.getTemplate().elements().getFirst().frame().bounds().x(), 0.0001);
            assertEquals(before + PX, sceneBounds(view(0)).getMinX(), 0.5);
        });
    }

    @Test
    void theResizeHandleSitsAtTheBottomRightCornerAndGrowsTheElementRightwards() {
        runFx(() -> {
            Bounds element = sceneBounds(view(0));
            Node handle = view(0).lookup(".resize-handle");
            Bounds handleBounds = sceneBounds(handle);
            assertTrue(handleBounds.getMinX() > element.getMinX() + element.getWidth() / 2,
                    "handle is on the element's right side: " + handleBounds + " vs " + element);
            assertTrue(handleBounds.getMinY() > element.getMinY() + element.getHeight() / 2,
                    "handle is on the element's bottom side");

            drag(handle, handleBounds.getCenterX(), handleBounds.getCenterY(), 10 * PX, 0);
            assertEquals(30, pane.getTemplate().elements().getFirst().frame().bounds().width(), 0.0001);
            assertEquals(element.getMinX(), sceneBounds(view(0)).getMinX(), 0.5, "the left edge stays put");
            assertEquals(element.getWidth() + 10 * PX, sceneBounds(view(0)).getWidth(), 0.5);
        });
    }

    @Test
    void elementsFurtherRightOnTheLabelAreFurtherRightOnScreen() {
        runFx(() -> {
            double labelLeft = sceneBounds(canvas()).getMinX();
            for (double xMm : new double[] {0, 2, 15, 30, 43.5}) {
                pane.setTemplate(FxTestSupport.template(List.of(FxTestSupport.text("Moving", new RectMm(xMm, 3, 20, 6), 0))));
                pane.applyCss();
                pane.layout();
                assertEquals(xMm * PX, view(0).localToScene(0, 0).getX() - labelLeft, 0.5, "element at x = " + xMm + " mm");
            }
        });
    }

    @Test
    void theRubberBandSelectsWhatItCoversOnScreen() {
        runFx(() -> {
            Bounds label = sceneBounds(canvas());
            // From 1 to 5 mm in from the label's top-left corner: over "First" (2..22 x 3..9 mm) only.
            double fromX = label.getMinX() + PX, fromY = label.getMinY() + PX;
            double toX = label.getMinX() + 5 * PX, toY = label.getMinY() + 5 * PX;
            mouse(canvas(), MouseEvent.MOUSE_PRESSED, fromX, fromY, false);
            mouse(canvas(), MouseEvent.MOUSE_DRAGGED, toX, toY, false);
            Bounds band = sceneBounds(canvas().lookup(".rubber-band-selection"));
            assertEquals(fromX, band.getMinX(), 0.5, "the band is drawn from where the drag started");
            assertEquals(toX, band.getMaxX(), 0.5, "to where the pointer is");
            mouse(canvas(), MouseEvent.MOUSE_RELEASED, toX, toY, false);

            assertTrue(selected(view(0)), "First is selected");
            assertFalse(selected(view(1)), "Second is not");
        });
    }

    @Test
    void theSmartGuideIsDrawnAtTheEdgeItSnapsTo() {
        runFx(() -> {
            canvas().setSnapEnabled(false);
            double labelLeft = sceneBounds(canvas()).getMinX();
            Bounds element = sceneBounds(view(0));
            double x = element.getCenterX(), y = element.getCenterY();
            // 27 mm to the right puts First's left edge at 29 mm, within snapping distance of Second's at 30 mm.
            mouse(view(0), MouseEvent.MOUSE_PRESSED, x, y, false);
            mouse(view(0), MouseEvent.MOUSE_DRAGGED, x + 27 * PX, y, false);
            Line guide = visibleGuide();
            assertEquals(30 * PX, sceneBounds(guide).getCenterX() - labelLeft, 0.5, "the guide is at Second's left edge");
            assertEquals(30 * PX, sceneBounds(view(0)).getMinX() - labelLeft, 0.5, "First has snapped to it");
            mouse(view(0), MouseEvent.MOUSE_RELEASED, x + 27 * PX, y, false);

            assertEquals(30, pane.getTemplate().elements().getFirst().frame().bounds().x(), 0.0001);
        });
    }

    @Test
    void arabicTextIsAlignedWithinItsElementAsItPrints() {
        runFx(() -> {
            pane.setTemplate(FxTestSupport.template(List.of(
                    arabic(TextAlignment.LEFT, 3),
                    arabic(TextAlignment.RIGHT, 15))));
            pane.applyCss();
            pane.layout();

            Bounds leftElement = sceneBounds(view(0));
            Bounds leftText = sceneBounds(view(0).lookup(".text"));
            assertEquals(leftElement.getMinX(), leftText.getMinX(), 1.5, "LEFT text starts at the element's left edge");
            assertTrue(leftElement.getMaxX() - leftText.getMaxX() > 50, "and leaves the space on its right");

            Bounds rightElement = sceneBounds(view(1));
            Bounds rightText = sceneBounds(view(1).lookup(".text"));
            assertEquals(rightElement.getMaxX(), rightText.getMaxX(), 1.5, "RIGHT text ends at the element's right edge");
            assertTrue(rightText.getMinX() - rightElement.getMinX() > 50, "and leaves the space on its left");
        });
    }

    private static TextElement arabic(TextAlignment alignment, double yMm) {
        return new TextElement(ElementFrame.create(alignment.name(),
                new RectMm(2, yMm, 55, 8), (int) yMm), "سعر المنتج", "Arial", 10, "#111827",
                false, false, alignment);
    }

    private static boolean selected(Node view) {
        return view.getStyleClass().contains("selected-element");
    }

    private Line visibleGuide() {
        return canvas().getChildren().stream()
                .filter(node -> node instanceof Line && node.getStyleClass().contains("smart-guide") && node.isVisible())
                .map(Line.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no smart guide is shown"));
    }

    private DesignerCanvas canvas() {
        return (DesignerCanvas) pane.lookup(".designer-canvas");
    }

    /** Element views in z order: 0 is "First" (x 2 mm), 1 is "Second" (x 30 mm). */
    private StackPane view(int index) {
        return canvas().getChildren().stream()
                .filter(node -> node.getStyleClass().contains("designer-element"))
                .map(StackPane.class::cast)
                .toList()
                .get(index);
    }

    private static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getLayoutBounds());
    }
}
