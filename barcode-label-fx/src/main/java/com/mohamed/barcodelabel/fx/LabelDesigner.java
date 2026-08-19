package com.mohamed.barcodelabel.fx;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.TextElement;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class LabelDesigner extends BorderPane {
    private static final double PIXELS_PER_MM = 4.0;

    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final Pane canvas = new Pane();
    private final Label emptyState = new Label("No label template loaded");

    public LabelDesigner() {
        getStyleClass().add("label-designer");
        canvas.getStyleClass().add("label-canvas");
        var scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        setCenter(scrollPane);
        BorderPane.setAlignment(emptyState, Pos.CENTER);
        template.addListener((observable, oldValue, newValue) -> renderTemplate(newValue));
        renderTemplate(null);
        loadDefaultStylesheet();
    }

    public ObjectProperty<LabelTemplate> templateProperty() {
        return template;
    }

    public LabelTemplate getTemplate() {
        return template.get();
    }

    public void setTemplate(LabelTemplate template) {
        this.template.set(template);
    }

    private void renderTemplate(LabelTemplate value) {
        canvas.getChildren().clear();
        if (value == null) {
            canvas.setPrefSize(640, 420);
            emptyState.relocate(220, 190);
            canvas.getChildren().add(emptyState);
            return;
        }

        var labelSize = value.page().labelSize();
        canvas.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        canvas.setPrefSize(labelSize.width() * PIXELS_PER_MM, labelSize.height() * PIXELS_PER_MM);
        value.elements().stream()
                .filter(element -> element.frame().visible())
                .sorted((left, right) -> Integer.compare(left.frame().zIndex(), right.frame().zIndex()))
                .map(this::createElementNode)
                .forEach(canvas.getChildren()::add);
    }

    private Node createElementNode(LabelElement element) {
        Node node = switch (element) {
            case TextElement text -> createText(text);
            case BarcodeElement barcode -> createBarcodePlaceholder(barcode);
            case ShapeElement shape -> createShape(shape);
            case ImageElement ignored -> createImagePlaceholder();
        };
        var bounds = element.frame().bounds();
        node.relocate(bounds.x() * PIXELS_PER_MM, bounds.y() * PIXELS_PER_MM);
        node.setRotate(element.frame().rotationDegrees());
        node.setManaged(false);
        return node;
    }

    private Node createText(TextElement element) {
        Text text = new Text(element.textTemplate());
        FontWeight weight = element.bold() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = element.italic() ? FontPosture.ITALIC : FontPosture.REGULAR;
        text.setFont(Font.font(element.fontFamily(), weight, posture, element.fontSizePoints()));
        text.setStyle("-fx-fill: " + element.color() + ";");
        return text;
    }

    private Node createBarcodePlaceholder(BarcodeElement element) {
        var bounds = element.frame().bounds();
        Label barcode = new Label(element.symbology() + "\n" + element.dataTemplate());
        barcode.getStyleClass().add("barcode-placeholder");
        barcode.setAlignment(Pos.CENTER);
        barcode.setPrefSize(bounds.width() * PIXELS_PER_MM, bounds.height() * PIXELS_PER_MM);
        return barcode;
    }

    private Node createShape(ShapeElement element) {
        var bounds = element.frame().bounds();
        javafx.scene.shape.Shape shape;
        if (element.shapeType() == com.mohamed.barcodelabel.model.element.ShapeType.ELLIPSE) {
            shape = new Ellipse(bounds.width() * PIXELS_PER_MM / 2, bounds.height() * PIXELS_PER_MM / 2);
        } else {
            Rectangle rectangle = new Rectangle(bounds.width() * PIXELS_PER_MM, bounds.height() * PIXELS_PER_MM);
            if (element.shapeType() == com.mohamed.barcodelabel.model.element.ShapeType.ROUNDED_RECTANGLE) {
                rectangle.setArcWidth(12);
                rectangle.setArcHeight(12);
            }
            shape = rectangle;
        }
        shape.setStyle("-fx-stroke: " + element.strokeColor() + "; -fx-fill: " + element.fillColor() + ";");
        shape.setStrokeWidth(element.strokeWidthMm() * PIXELS_PER_MM);
        return shape;
    }

    private Node createImagePlaceholder() {
        Label image = new Label("IMAGE");
        image.getStyleClass().add("image-placeholder");
        return image;
    }

    private void loadDefaultStylesheet() {
        var resource = LabelDesigner.class.getResource("label-designer.css");
        if (resource != null) {
            getStylesheets().add(resource.toExternalForm());
        }
    }
}

