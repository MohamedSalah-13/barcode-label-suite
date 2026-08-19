package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class DesignerCanvas extends Pane {
    private static final double BASE_PIXELS_PER_MM = 4.0;
    private final TemplateEditSession session;
    private final ElementViewFactory viewFactory = new ElementViewFactory();
    private final Canvas gridCanvas = new Canvas();
    private final Map<UUID, StackPane> elementViews = new HashMap<>();
    private final Map<UUID, RectMm> dragStartBounds = new HashMap<>();
    private LabelTemplate renderedTemplate;
    private double zoom = 1;
    private double gridMm = 1;
    private boolean snapEnabled = true;
    private double dragStartSceneX;
    private double dragStartSceneY;
    private double currentDeltaX;
    private double currentDeltaY;

    DesignerCanvas(TemplateEditSession session) {
        this.session = session;
        getStyleClass().add("designer-canvas");
        setFocusTraversable(true);
        getChildren().add(gridCanvas);
        session.addListener(this::sessionChanged);
        setOnMousePressed(event -> {
            if (event.getTarget() == this || event.getTarget() == gridCanvas) session.clearSelection();
            requestFocus();
        });
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        rebuild();
    }

    double pixelsPerMm() { return BASE_PIXELS_PER_MM * zoom; }
    void setZoom(double value) { zoom = Math.max(0.5, Math.min(4, value)); rebuild(); }
    void setGridMm(double value) { gridMm = Math.max(0.1, value); redrawGrid(); }
    void setSnapEnabled(boolean value) { snapEnabled = value; }

    private void sessionChanged() {
        if (renderedTemplate != session.template()) rebuild(); else refreshSelectionStyles();
    }

    private void rebuild() {
        renderedTemplate = session.template();
        elementViews.clear();
        getChildren().setAll(gridCanvas);
        var size = renderedTemplate.page().labelSize();
        double width = size.width() * pixelsPerMm();
        double height = size.height() * pixelsPerMm();
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        gridCanvas.setWidth(width);
        gridCanvas.setHeight(height);
        redrawGrid();
        renderedTemplate.elements().stream()
                .filter(element -> element.frame().visible())
                .sorted((left, right) -> Integer.compare(left.frame().zIndex(), right.frame().zIndex()))
                .forEach(this::addElementView);
        refreshSelectionStyles();
    }

    private void redrawGrid() {
        GraphicsContext graphics = gridCanvas.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());
        double step = gridMm * pixelsPerMm();
        graphics.setStroke(Color.rgb(203, 213, 225, 0.65));
        graphics.setLineWidth(0.5);
        for (double x = step; x < gridCanvas.getWidth(); x += step) graphics.strokeLine(x, 0, x, gridCanvas.getHeight());
        for (double y = step; y < gridCanvas.getHeight(); y += step) graphics.strokeLine(0, y, gridCanvas.getWidth(), y);
    }

    private void addElementView(LabelElement element) {
        RectMm bounds = element.frame().bounds();
        StackPane wrapper = new StackPane(viewFactory.create(element, pixelsPerMm()));
        wrapper.getStyleClass().add("designer-element");
        wrapper.setManaged(false);
        wrapper.relocate(bounds.x() * pixelsPerMm(), bounds.y() * pixelsPerMm());
        wrapper.setPrefSize(bounds.width() * pixelsPerMm(), bounds.height() * pixelsPerMm());
        wrapper.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setRotate(element.frame().rotationDegrees());
        if (element.frame().locked()) wrapper.getStyleClass().add("locked-element");

        Region handle = new Region();
        handle.getStyleClass().add("resize-handle");
        handle.setPrefSize(10, 10);
        handle.setMaxSize(10, 10);
        StackPane.setAlignment(handle, javafx.geometry.Pos.BOTTOM_RIGHT);
        handle.setTranslateX(5);
        handle.setTranslateY(5);
        handle.visibleProperty().bind(wrapper.focusedProperty());
        wrapper.getChildren().add(handle);

        installMoveHandlers(wrapper, element);
        installResizeHandlers(wrapper, handle, element);
        elementViews.put(element.frame().id(), wrapper);
        getChildren().add(wrapper);
    }

    private void installMoveHandlers(StackPane wrapper, LabelElement element) {
        wrapper.setOnMousePressed(event -> {
            if (event.getTarget().getClass().equals(Region.class)) return;
            session.select(element.frame().id(), event.isControlDown());
            requestFocus();
            dragStartSceneX = event.getSceneX();
            dragStartSceneY = event.getSceneY();
            currentDeltaX = currentDeltaY = 0;
            dragStartBounds.clear();
            session.selection().forEach(id -> {
                LabelElement selected = session.find(id);
                if (selected != null) dragStartBounds.put(id, selected.frame().bounds());
            });
            event.consume();
        });
        wrapper.setOnMouseDragged(event -> {
            if (element.frame().locked() || dragStartBounds.isEmpty()) return;
            currentDeltaX = snapDelta((event.getSceneX() - dragStartSceneX) / pixelsPerMm());
            currentDeltaY = snapDelta((event.getSceneY() - dragStartSceneY) / pixelsPerMm());
            dragStartBounds.forEach((id, bounds) -> {
                LabelElement selected = session.find(id);
                StackPane view = elementViews.get(id);
                if (view != null && selected != null && !selected.frame().locked()) {
                    view.relocate((bounds.x() + currentDeltaX) * pixelsPerMm(),
                            (bounds.y() + currentDeltaY) * pixelsPerMm());
                }
            });
            event.consume();
        });
        wrapper.setOnMouseReleased(event -> {
            if (!dragStartBounds.isEmpty() && (currentDeltaX != 0 || currentDeltaY != 0)) {
                session.moveSelected(currentDeltaX, currentDeltaY);
            }
            dragStartBounds.clear();
            event.consume();
        });
    }

    private void installResizeHandlers(StackPane wrapper, Region handle, LabelElement element) {
        final RectMm[] start = new RectMm[1];
        final double[] mouse = new double[2];
        handle.setOnMousePressed(event -> {
            if (element.frame().locked()) return;
            session.select(element.frame().id(), false);
            start[0] = element.frame().bounds();
            mouse[0] = event.getSceneX();
            mouse[1] = event.getSceneY();
            event.consume();
        });
        handle.setOnMouseDragged(event -> {
            if (start[0] == null) return;
            double width = Math.max(2, start[0].width() + snapDelta((event.getSceneX() - mouse[0]) / pixelsPerMm()));
            double height = Math.max(2, start[0].height() + snapDelta((event.getSceneY() - mouse[1]) / pixelsPerMm()));
            wrapper.setPrefSize(width * pixelsPerMm(), height * pixelsPerMm());
            event.consume();
        });
        handle.setOnMouseReleased(event -> {
            if (start[0] != null) {
                double width = wrapper.getPrefWidth() / pixelsPerMm();
                double height = wrapper.getPrefHeight() / pixelsPerMm();
                session.resize(element.frame().id(), new RectMm(start[0].x(), start[0].y(), width, height));
                start[0] = null;
            }
            event.consume();
        });
    }

    private void refreshSelectionStyles() {
        elementViews.forEach((id, view) -> {
            boolean selected = session.selection().contains(id);
            if (selected && !view.getStyleClass().contains("selected-element")) view.getStyleClass().add("selected-element");
            if (!selected) view.getStyleClass().remove("selected-element");
            view.setFocusTraversable(selected);
            if (selected) view.requestFocus();
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) { session.undo(); event.consume(); return; }
        if (event.isControlDown() && (event.getCode() == KeyCode.Y || (event.isShiftDown() && event.getCode() == KeyCode.Z))) {
            session.redo(); event.consume(); return;
        }
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            session.deleteSelected(); event.consume(); return;
        }
        double step = event.isShiftDown() ? 1 : 0.1;
        switch (event.getCode()) {
            case LEFT -> session.moveSelected(-step, 0);
            case RIGHT -> session.moveSelected(step, 0);
            case UP -> session.moveSelected(0, -step);
            case DOWN -> session.moveSelected(0, step);
            default -> { return; }
        }
        event.consume();
    }

    private double snapDelta(double value) {
        if (!snapEnabled) return value;
        return Math.round(value / gridMm) * gridMm;
    }
}

