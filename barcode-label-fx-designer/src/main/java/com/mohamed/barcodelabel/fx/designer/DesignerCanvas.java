package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.geometry.NodeOrientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class DesignerCanvas extends Pane {
    private static final double BASE_PIXELS_PER_MM = 4.0;
    private static final double SNAP_TOLERANCE_PX = 6;
    private final TemplateEditSession session;
    private final UiTexts texts;
    private final ElementViewFactory viewFactory = new ElementViewFactory();
    private final Canvas gridCanvas = new Canvas();
    private final Map<UUID, StackPane> elementViews = new HashMap<>();
    private final Map<UUID, RectMm> dragStartBounds = new HashMap<>();
    private final Rectangle rubberBand = new Rectangle();
    private final Line verticalGuide = new Line();
    private final Line horizontalGuide = new Line();
    private LabelTemplate renderedTemplate;
    private double zoom = 1;
    private double gridMm = 1;
    private boolean snapEnabled = true;
    private double dragStartSceneX;
    private double dragStartSceneY;
    private double currentDeltaX;
    private double currentDeltaY;
    private double rubberBandStartX;
    private double rubberBandStartY;

    DesignerCanvas(TemplateEditSession session) {
        this(session, UiTexts.load(DesignerCanvas.class, "messages", UiLanguage.ARABIC));
    }

    DesignerCanvas(TemplateEditSession session, UiTexts texts) {
        this.session = session;
        this.texts = texts;
        // The label's coordinates run left to right in millimetres, as it prints. Hosts such as
        // LabelDesignerPane are right to left for Arabic; inheriting that would mirror the label, put the
        // resize handle on the left and make dragging and the arrow keys move elements the wrong way.
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        getStyleClass().add("designer-canvas");
        setFocusTraversable(true);
        getChildren().add(gridCanvas);
        rubberBand.getStyleClass().add("rubber-band-selection");
        rubberBand.setManaged(false);
        rubberBand.setMouseTransparent(true);
        rubberBand.setVisible(false);
        rubberBand.setFill(Color.rgb(59, 130, 246, 0.15));
        rubberBand.setStroke(Color.rgb(59, 130, 246, 0.9));
        rubberBand.setStrokeWidth(1);
        rubberBand.getStrokeDashArray().addAll(4.0, 4.0);
        for (Line guide : List.of(verticalGuide, horizontalGuide)) {
            guide.getStyleClass().add("smart-guide");
            guide.setManaged(false);
            guide.setMouseTransparent(true);
            guide.setVisible(false);
            guide.setStroke(Color.rgb(236, 72, 153, 0.9));
            guide.setStrokeWidth(1);
        }
        session.addListener(this::sessionChanged);
        setOnMousePressed(event -> {
            if (event.getTarget() == this || event.getTarget() == gridCanvas) {
                if (!event.isControlDown()) session.clearSelection();
                rubberBandStartX = event.getX();
                rubberBandStartY = event.getY();
                rubberBand.setX(rubberBandStartX);
                rubberBand.setY(rubberBandStartY);
                rubberBand.setWidth(0);
                rubberBand.setHeight(0);
                rubberBand.setVisible(true);
            }
            requestFocus();
        });
        setOnMouseDragged(event -> {
            if (!rubberBand.isVisible()) return;
            double x = Math.max(0, Math.min(event.getX(), getWidth()));
            double y = Math.max(0, Math.min(event.getY(), getHeight()));
            rubberBand.setX(Math.min(rubberBandStartX, x));
            rubberBand.setY(Math.min(rubberBandStartY, y));
            rubberBand.setWidth(Math.abs(x - rubberBandStartX));
            rubberBand.setHeight(Math.abs(y - rubberBandStartY));
        });
        setOnMouseReleased(event -> {
            if (!rubberBand.isVisible()) return;
            rubberBand.setVisible(false);
            if (rubberBand.getWidth() > 3 || rubberBand.getHeight() > 3) {
                selectElementsInRubberBand(event.isControlDown());
            }
        });
        setOnContextMenuRequested(event -> {
            if (event.getTarget() == this || event.getTarget() == gridCanvas) {
                buildCanvasContextMenu().show(this, event.getScreenX(), event.getScreenY());
                event.consume();
            }
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
        addDuplicatePreview();
        getChildren().addAll(rubberBand, verticalGuide, horizontalGuide);
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
        // The wrapper is unmanaged, so no layout pass sizes it: without this it stays 0 x 0, which hides
        // its content, its selection border and its resize handle, and breaks rubber-band hit testing.
        wrapper.autosize();
        var origin = renderedTemplate.contentAreas().getFirst();
        wrapper.setTranslateX(origin.x() * pixelsPerMm());
        wrapper.setTranslateY(origin.y() * pixelsPerMm());
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
        wrapper.setOnContextMenuRequested(event -> {
            if (!session.selection().contains(element.frame().id())) {
                session.select(element.frame().id(), false);
            }
            buildElementContextMenu(element.frame().id()).show(wrapper, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        elementViews.put(element.frame().id(), wrapper);
        getChildren().add(wrapper);
    }

    private void addDuplicatePreview() {
        var layout = renderedTemplate.dualLayout();
        if (layout == null) return;
        var areas = renderedTemplate.contentAreas();
        for (LabelElement element : renderedTemplate.elements().stream()
                .filter(e -> e.frame().visible()).sorted(java.util.Comparator.comparingInt(e -> e.frame().zIndex())).toList()) {
            var source = elementViews.get(element.frame().id());
            var copy = new StackPane(viewFactory.create(element, pixelsPerMm()));
            copy.getStyleClass().add("dual-label-copy");
            copy.setManaged(false);
            copy.setMouseTransparent(true);
            copy.layoutXProperty().bind(source.layoutXProperty());
            copy.layoutYProperty().bind(source.layoutYProperty());
            copy.setTranslateX(areas.get(1).x() * pixelsPerMm());
            copy.setTranslateY(areas.get(1).y() * pixelsPerMm());
            copy.setPrefSize(source.getPrefWidth(), source.getPrefHeight());
            copy.resize(source.getPrefWidth(), source.getPrefHeight());
            copy.setRotate(element.frame().rotationDegrees());
            getChildren().add(copy);
        }
        if (layout.showGuide()) {
            double y = layout.splitAt(renderedTemplate.page().labelSize()) * pixelsPerMm();
            Line divider = new Line(0, y, getPrefWidth(), y);
            divider.setId("dual-label-guide");
            divider.setManaged(false);
            divider.setMouseTransparent(true);
            divider.setStroke(Color.web(layout.guideColor()));
            if (layout.dashedGuide()) divider.getStrokeDashArray().setAll(5.0, 4.0);
            getChildren().add(divider);
        }
    }

    private ContextMenu buildElementContextMenu(UUID id) {
        MenuItem copy = new MenuItem(texts.get("designer.copy"));
        copy.setOnAction(event -> session.copySelected());
        MenuItem cut = new MenuItem(texts.get("designer.cut"));
        cut.setOnAction(event -> { session.copySelected(); session.deleteSelected(); });
        MenuItem paste = new MenuItem(texts.get("designer.paste"));
        paste.setDisable(!session.hasClipboard());
        paste.setOnAction(event -> session.pasteClipboard());
        MenuItem duplicate = new MenuItem(texts.get("designer.duplicate"));
        duplicate.setOnAction(event -> session.duplicateSelected(2));
        MenuItem delete = new MenuItem(texts.get("designer.delete"));
        delete.setOnAction(event -> session.deleteSelected());
        MenuItem toFront = new MenuItem(texts.get("designer.bringToFront"));
        toFront.setOnAction(event -> session.bringToFront(id));
        MenuItem forward = new MenuItem(texts.get("designer.bringForward"));
        forward.setOnAction(event -> session.bringForward(id));
        MenuItem backward = new MenuItem(texts.get("designer.sendBackward"));
        backward.setOnAction(event -> session.sendBackward(id));
        MenuItem toBack = new MenuItem(texts.get("designer.sendToBack"));
        toBack.setOnAction(event -> session.sendToBack(id));
        return new ContextMenu(cut, copy, paste, duplicate, delete,
                new SeparatorMenuItem(), toFront, forward, backward, toBack);
    }

    private ContextMenu buildCanvasContextMenu() {
        MenuItem paste = new MenuItem(texts.get("designer.paste"));
        paste.setDisable(!session.hasClipboard());
        paste.setOnAction(event -> session.pasteClipboard());
        return new ContextMenu(paste);
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
            RectMm startBounds = dragStartBounds.get(element.frame().id());
            if (startBounds != null) {
                RectMm tentative = new RectMm(startBounds.x() + currentDeltaX, startBounds.y() + currentDeltaY,
                        startBounds.width(), startBounds.height());
                updateSmartGuides(tentative);
            }
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
            verticalGuide.setVisible(false);
            horizontalGuide.setVisible(false);
            event.consume();
        });
    }

    private void updateSmartGuides(RectMm tentative) {
        verticalGuide.setVisible(false);
        horizontalGuide.setVisible(false);
        double toleranceMm = SNAP_TOLERANCE_PX / pixelsPerMm();
        double left = tentative.x();
        double centerX = tentative.x() + tentative.width() / 2;
        double right = tentative.x() + tentative.width();
        double top = tentative.y();
        double centerY = tentative.y() + tentative.height() / 2;
        double bottom = tentative.y() + tentative.height();

        Double snapDeltaX = null;
        Double guideXmm = null;
        Double snapDeltaY = null;
        Double guideYmm = null;

        var labelSize = renderedTemplate.designSize();
        double[][] horizontalTargets = {
                {left, 0},
                {centerX, labelSize.width() / 2},
                {right, labelSize.width()}
        };
        for (double[] pair : horizontalTargets) {
            double diff = pair[1] - pair[0];
            if (Math.abs(diff) <= toleranceMm && (snapDeltaX == null || Math.abs(diff) < Math.abs(snapDeltaX))) {
                snapDeltaX = diff;
                guideXmm = pair[1];
            }
        }
        double[][] verticalTargets = {
                {top, 0},
                {centerY, labelSize.height() / 2},
                {bottom, labelSize.height()}
        };
        for (double[] pair : verticalTargets) {
            double diff = pair[1] - pair[0];
            if (Math.abs(diff) <= toleranceMm && (snapDeltaY == null || Math.abs(diff) < Math.abs(snapDeltaY))) {
                snapDeltaY = diff;
                guideYmm = pair[1];
            }
        }

        for (LabelElement other : renderedTemplate.elements()) {
            if (session.selection().contains(other.frame().id())) continue;
            RectMm b = other.frame().bounds();
            for (double[] pair : new double[][]{{left, b.x()}, {centerX, b.x() + b.width() / 2}, {right, b.x() + b.width()}}) {
                double diff = pair[1] - pair[0];
                if (Math.abs(diff) <= toleranceMm && (snapDeltaX == null || Math.abs(diff) < Math.abs(snapDeltaX))) {
                    snapDeltaX = diff;
                    guideXmm = pair[1];
                }
            }
            for (double[] pair : new double[][]{{top, b.y()}, {centerY, b.y() + b.height() / 2}, {bottom, b.y() + b.height()}}) {
                double diff = pair[1] - pair[0];
                if (Math.abs(diff) <= toleranceMm && (snapDeltaY == null || Math.abs(diff) < Math.abs(snapDeltaY))) {
                    snapDeltaY = diff;
                    guideYmm = pair[1];
                }
            }
        }

        if (snapDeltaX != null) {
            currentDeltaX += snapDeltaX;
            double x = (guideXmm + renderedTemplate.contentAreas().getFirst().x()) * pixelsPerMm();
            verticalGuide.setStartX(x); verticalGuide.setEndX(x);
            verticalGuide.setStartY(0); verticalGuide.setEndY(getHeight());
            verticalGuide.setVisible(true);
        }
        if (snapDeltaY != null) {
            currentDeltaY += snapDeltaY;
            double y = (guideYmm + renderedTemplate.contentAreas().getFirst().y()) * pixelsPerMm();
            horizontalGuide.setStartY(y); horizontalGuide.setEndY(y);
            horizontalGuide.setStartX(0); horizontalGuide.setEndX(getWidth());
            horizontalGuide.setVisible(true);
        }
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
            wrapper.autosize();
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

    private void selectElementsInRubberBand(boolean additive) {
        var band = rubberBand.getBoundsInParent();
        var matched = elementViews.entrySet().stream()
                .filter(entry -> entry.getValue().getBoundsInParent().intersects(band))
                .map(Map.Entry::getKey)
                .toList();
        if (!additive) session.clearSelection();
        var alreadySelected = session.selection();
        for (UUID id : matched) {
            if (!alreadySelected.contains(id)) session.select(id, true);
        }
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
        if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.Z) {
            session.undo(); event.consume(); return;
        }
        if (event.isControlDown() && (event.getCode() == KeyCode.Y || (event.isShiftDown() && event.getCode() == KeyCode.Z))) {
            session.redo(); event.consume(); return;
        }
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            session.deleteSelected(); event.consume(); return;
        }
        if (event.isControlDown() && event.getCode() == KeyCode.C) { session.copySelected(); event.consume(); return; }
        if (event.isControlDown() && event.getCode() == KeyCode.V) { session.pasteClipboard(); event.consume(); return; }
        if (event.isControlDown() && event.getCode() == KeyCode.X) {
            session.copySelected(); session.deleteSelected(); event.consume(); return;
        }
        if (event.isControlDown() && event.getCode() == KeyCode.D) { session.duplicateSelected(2); event.consume(); return; }
        if (event.isControlDown() && event.getCode() == KeyCode.A) { session.selectAll(); event.consume(); return; }
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

