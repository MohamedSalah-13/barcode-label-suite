package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.color.LabelColors;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.render.java2d.CompositeBarcodeRasterizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.text.Text;

import java.io.File;

final class ElementViewFactory {
    private static final double MM_PER_POINT = 25.4 / 72;

    private final CompositeBarcodeRasterizer barcodeRasterizer = CompositeBarcodeRasterizer.standard();

    Node create(LabelElement element, double pixelsPerMm) {
        double width = element.frame().bounds().width() * pixelsPerMm;
        double height = element.frame().bounds().height() * pixelsPerMm;
        Node content = switch (element) {
            case TextElement text -> textView(text, width, height, pixelsPerMm);
            case BarcodeElement barcode -> barcodeView(barcode, width, height);
            case ShapeElement shape -> shapeView(shape, width, height, pixelsPerMm);
            case ImageElement image -> imageView(image, width, height);
        };
        content.setMouseTransparent(true);
        return content;
    }

    /**
     * The canvas pixel size of a font, so text is as large relative to the label as it prints: the Java2D renderer
     * draws {@code points * dpi / 72} pixels, and the canvas has {@code pixelsPerMm * 25.4} pixels per inch.
     */
    static double fontPixels(double points, double pixelsPerMm) {
        return points * MM_PER_POINT * pixelsPerMm;
    }

    /**
     * Text laid out as BufferedImageLabelRenderer prints it: the whole string on one line, placed by its full width
     * at its alignment, its line vertically centred (or at the top when taller than the frame), and cut off at the
     * element's frame. A clipped Label would instead keep the part that fits and align that, showing other
     * characters than the printout when right-aligned or centred text overflows.
     */
    private Node textView(TextElement element, double width, double height, double pixelsPerMm) {
        Text text = new Text(element.textTemplate());
        text.getStyleClass().add("element-text");
        text.setFill(parseColor(element.color()));
        text.setFont(Font.font(element.fontFamily(), element.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
                element.italic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                fontPixels(element.fontSizePoints(), pixelsPerMm)));
        text.setTextOrigin(VPos.TOP);
        Bounds line = text.getLayoutBounds();
        text.setX(switch (element.alignment()) {
            case LEFT -> 0;
            case CENTER -> (width - line.getWidth()) / 2;
            case RIGHT -> width - line.getWidth();
        });
        text.setY(Math.max(0, (height - line.getHeight()) / 2));

        Pane frame = new Pane(text);
        frame.setMinSize(0, 0);
        frame.setPrefSize(width, height);
        frame.setMaxSize(width, height);
        frame.setClip(new Rectangle(width, height));
        return frame;
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

    /** Reads colours exactly as printing does (see LabelColors), so the label shows the colours it prints. */
    static Color parseColor(String value) {
        int argb = LabelColors.toArgbOrBlack(value);
        return Color.rgb(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, (argb >>> 24) / 255.0);
    }
}

