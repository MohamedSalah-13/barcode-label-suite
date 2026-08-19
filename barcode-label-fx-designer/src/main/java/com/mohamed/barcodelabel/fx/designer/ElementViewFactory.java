package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.render.java2d.CompositeBarcodeRasterizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.io.File;

final class ElementViewFactory {
    private final CompositeBarcodeRasterizer barcodeRasterizer = CompositeBarcodeRasterizer.standard();

    Node create(LabelElement element, double pixelsPerMm) {
        double width = element.frame().bounds().width() * pixelsPerMm;
        double height = element.frame().bounds().height() * pixelsPerMm;
        Node content = switch (element) {
            case TextElement text -> textView(text, width, height);
            case BarcodeElement barcode -> barcodeView(barcode, width, height);
            case ShapeElement shape -> shapeView(shape, width, height, pixelsPerMm);
            case ImageElement image -> imageView(image, width, height);
        };
        content.setMouseTransparent(true);
        return content;
    }

    private Node textView(TextElement element, double width, double height) {
        Label label = new Label(element.textTemplate());
        label.setWrapText(true);
        label.setPrefSize(width, height);
        label.setMinSize(0, 0);
        label.setMaxSize(width, height);
        label.setTextFill(parseColor(element.color()));
        label.setFont(Font.font(element.fontFamily(), element.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
                element.italic() ? FontPosture.ITALIC : FontPosture.REGULAR, element.fontSizePoints()));
        label.setAlignment(switch (element.alignment()) {
            case LEFT -> Pos.CENTER_LEFT;
            case CENTER -> Pos.CENTER;
            case RIGHT -> Pos.CENTER_RIGHT;
        });
        return label;
    }

    private Node barcodeView(BarcodeElement element, double width, double height) {
        try {
            double dpi = 150;
            int rasterWidth = Math.max(1, (int) Math.round(element.frame().bounds().width() * dpi / 25.4));
            int rasterHeight = Math.max(1, (int) Math.round(element.frame().bounds().height() * dpi / 25.4));
            var buffered = barcodeRasterizer.rasterize(element, rasterWidth, rasterHeight, dpi);
            ImageView image = new ImageView(SwingFXUtils.toFXImage(buffered, null));
            image.setPreserveRatio(true);
            image.setSmooth(false);
            image.setFitWidth(width);
            image.setFitHeight(height);
            return image;
        } catch (Exception exception) {
            Label error = new Label(element.symbology() + "\n" + element.dataTemplate());
            error.setAlignment(Pos.CENTER);
            error.setPrefSize(width, height);
            error.getStyleClass().add("barcode-error");
            return error;
        }
    }

    private Node shapeView(ShapeElement element, double width, double height, double pixelsPerMm) {
        javafx.scene.shape.Shape shape = switch (element.shapeType()) {
            case LINE -> new Line(0, 0, width, height);
            case RECTANGLE, ROUNDED_RECTANGLE -> {
                Rectangle rectangle = new Rectangle(width, height);
                if (element.shapeType() == ShapeType.ROUNDED_RECTANGLE) {
                    rectangle.setArcWidth(Math.min(width, height) * 0.2);
                    rectangle.setArcHeight(Math.min(width, height) * 0.2);
                }
                yield rectangle;
            }
            case ELLIPSE -> new Ellipse(width / 2, height / 2, width / 2, height / 2);
        };
        shape.setFill(parseColor(element.fillColor()));
        shape.setStroke(parseColor(element.strokeColor()));
        shape.setStrokeWidth(Math.max(0.5, element.strokeWidthMm() * pixelsPerMm));
        Pane holder = new Pane(shape);
        holder.setPrefSize(width, height);
        return holder;
    }

    private Node imageView(ImageElement element, double width, double height) {
        try {
            Image image = element.source().startsWith("file:")
                    ? new Image(element.source(), true)
                    : new Image(new File(element.source()).toURI().toString(), true);
            ImageView view = new ImageView(image);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setPreserveRatio(element.fit() != com.mohamed.barcodelabel.model.element.ImageFit.STRETCH);
            return view;
        } catch (Exception exception) {
            Label placeholder = new Label("IMAGE");
            placeholder.setPrefSize(width, height);
            placeholder.setAlignment(Pos.CENTER);
            placeholder.getStyleClass().add("image-error");
            return placeholder;
        }
    }

    private static Color parseColor(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("transparent")) return Color.TRANSPARENT;
        try { return Color.web(value); } catch (IllegalArgumentException ignored) { return Color.BLACK; }
    }
}

