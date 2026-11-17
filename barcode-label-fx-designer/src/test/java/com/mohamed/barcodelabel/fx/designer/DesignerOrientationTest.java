package com.mohamed.barcodelabel.fx.designer;

import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.drag;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.key;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
