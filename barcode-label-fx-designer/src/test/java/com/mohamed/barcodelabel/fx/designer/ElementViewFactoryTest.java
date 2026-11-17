package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ElementViewFactoryTest {
    private static final double PX_PER_MM = 4;
    private static final RectMm BOUNDS = new RectMm(1, 1, 30, 10);

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void textUsesTheElementsStyleAndAlignment() {
        var element = new TextElement(frame(), "Hello", "Arial", 12, "#FF0000", true, true, TextAlignment.RIGHT);
        Label label = assertInstanceOf(Label.class, create(element));

        assertEquals("Hello", label.getText());
        assertEquals(Color.web("#FF0000"), label.getTextFill());
        assertEquals(Pos.CENTER_RIGHT, label.getAlignment());
        // 12 pt is 12/72 inch = 4.233 mm, at 4 px per mm.
        assertEquals(12 * 25.4 / 72 * PX_PER_MM, label.getFont().getSize(), 0.001);
        assertEquals(30 * PX_PER_MM, label.getPrefWidth(), 0.001);
        assertTrue(label.isMouseTransparent(), "element content must not steal mouse events from its wrapper");
    }

    @Test
    void textSizeFollowsTheZoom() {
        var element = new TextElement(frame(), "Hello", "Arial", 10, "#000000", false, false, TextAlignment.LEFT);
        Label atOne = (Label) create(element);
        Label atTwo = (Label) onFx(() -> new ElementViewFactory().create(element, 2 * PX_PER_MM));
        assertEquals(2 * atOne.getFont().getSize(), atTwo.getFont().getSize(), 0.001);
    }

    @Test
    void textIsOneLineCutOffAtTheFrameAsItPrints() {
        Label label = (Label) create(textAligned(TextAlignment.LEFT));
        assertFalse(label.isWrapText());
        assertEquals(OverrunStyle.CLIP, label.getTextOverrun());
    }

    @Test
    void textIsAsWideOnTheCanvasAsItPrints() {
        String family = installedInBothToolkits("Arial", "Liberation Sans", "DejaVu Sans");
        String value = "Barcode label 0123456789";
        double dpi = PX_PER_MM * 25.4;
        var element = new TextElement(ElementFrame.create("wide", new RectMm(0, 0, 150, 10), 0),
                value, family, 10, "#000000", false, false, TextAlignment.LEFT);

        // As BufferedImageLabelRenderer draws it.
        java.awt.Font awtFont = new java.awt.Font(family, java.awt.Font.PLAIN, 1).deriveFont((float) (10 * dpi / 72));
        double printed = new TextLayout(value, awtFont, new FontRenderContext(null, true, true)).getAdvance();
        double onCanvas = onFx(() -> {
            Label label = (Label) new ElementViewFactory().create(element, PX_PER_MM);
            Text text = new Text(value);
            text.setFont(label.getFont());
            return text.getLayoutBounds().getWidth();
        });
        // Before the font was scaled to the canvas resolution, the canvas text was about 29% narrower.
        assertEquals(1, onCanvas / printed, 0.03, family + ": canvas " + onCanvas + " px vs printed " + printed + " px");
    }

    /** A font family both Java2D and JavaFX have, so neither substitutes another font. */
    private static String installedInBothToolkits(String... families) {
        Set<String> awt = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        List<String> fx = onFx(Font::getFamilies);
        Optional<String> family = Arrays.stream(families)
                .filter(name -> awt.contains(name) && fx.contains(name))
                .findFirst();
        assumeTrue(family.isPresent(), "none of " + Arrays.toString(families) + " is installed for both toolkits");
        return family.get();
    }

    @Test
    void textAlignmentMapsLeftAndCenter() {
        assertEquals(Pos.CENTER_LEFT, ((Label) create(textAligned(TextAlignment.LEFT))).getAlignment());
        assertEquals(Pos.CENTER, ((Label) create(textAligned(TextAlignment.CENTER))).getAlignment());
    }

    @Test
    void invalidOrTransparentColorsFallBack() {
        var badText = new TextElement(frame(), "x", "Arial", 10, "not-a-color", false, false, TextAlignment.LEFT);
        assertEquals(Color.BLACK, ((Label) create(badText)).getTextFill());

        var transparentShape = new ShapeElement(frame(), ShapeType.RECTANGLE, "", "transparent", 0.3);
        Shape shape = shapeOf(create(transparentShape));
        assertEquals(Color.TRANSPARENT, shape.getFill());
        assertEquals(Color.TRANSPARENT, shape.getStroke());
    }

    @Test
    void validBarcodeIsRasterizedToAnImage() {
        var element = new BarcodeElement(frame(), "12345", Symbology.CODE_128, 0.25, 2.5, true);
        ImageView view = assertInstanceOf(ImageView.class, create(element));

        assertNotNull(view.getImage());
        assertTrue(view.getImage().getWidth() > 0);
        assertTrue(view.isPreserveRatio());
        assertEquals(30 * PX_PER_MM, view.getFitWidth(), 0.001);
    }

    @Test
    void barcodeThatCannotBeEncodedShowsAnErrorPlaceholder() {
        // EAN-13 accepts digits only.
        var element = new BarcodeElement(frame(), "not digits", Symbology.EAN_13, 0.25, 2.5, true);
        Label label = assertInstanceOf(Label.class, create(element));

        assertTrue(label.getStyleClass().contains("barcode-error"));
        assertEquals("EAN_13\nnot digits", label.getText());
    }

    @Test
    void barcodeTooWideForItsFrameShowsAnErrorPlaceholder() {
        // At 0.25 mm per module, CODE_128 "ABC-12345" needs about 35 mm; the frame is 30 mm wide.
        var element = new BarcodeElement(frame(), "ABC-12345", Symbology.CODE_128, 0.25, 2.5, true);
        Label label = assertInstanceOf(Label.class, create(element));
        assertTrue(label.getStyleClass().contains("barcode-error"));
    }

    @Test
    void eachShapeTypeProducesTheMatchingShape() {
        assertInstanceOf(Line.class, shapeOf(create(shape(ShapeType.LINE))));
        Rectangle square = assertInstanceOf(Rectangle.class, shapeOf(create(shape(ShapeType.RECTANGLE))));
        assertEquals(0, square.getArcWidth());
        Rectangle rounded = assertInstanceOf(Rectangle.class, shapeOf(create(shape(ShapeType.ROUNDED_RECTANGLE))));
        assertTrue(rounded.getArcWidth() > 0);
        Ellipse ellipse = assertInstanceOf(Ellipse.class, shapeOf(create(shape(ShapeType.ELLIPSE))));
        assertEquals(15 * PX_PER_MM, ellipse.getRadiusX(), 0.001);
    }

    @Test
    void shapeStrokeIsScaledButNeverThinnerThanHalfAPixel() {
        Shape thick = shapeOf(create(new ShapeElement(frame(), ShapeType.RECTANGLE, "#000000", "#FFFFFF", 0.5)));
        assertEquals(0.5 * PX_PER_MM, thick.getStrokeWidth(), 0.001);
        Shape hairline = shapeOf(create(new ShapeElement(frame(), ShapeType.RECTANGLE, "#000000", "#FFFFFF", 0)));
        assertEquals(0.5, hairline.getStrokeWidth(), 0.001);
    }

    @Test
    void imageFromAPathOrFileUrlBecomesAnImageView(@TempDir Path directory) {
        Path missing = directory.resolve("missing.png");

        ImageView fromPath = assertInstanceOf(ImageView.class,
                create(new ImageElement(frame(), missing.toString(), ImageFit.CONTAIN)));
        assertTrue(fromPath.isPreserveRatio());
        assertEquals(30 * PX_PER_MM, fromPath.getFitWidth(), 0.001);

        ImageView fromUrl = assertInstanceOf(ImageView.class,
                create(new ImageElement(frame(), missing.toUri().toString(), ImageFit.STRETCH)));
        assertFalse(fromUrl.isPreserveRatio(), "STRETCH must not preserve the aspect ratio");
    }

    private static Node create(com.mohamed.barcodelabel.model.element.LabelElement element) {
        return onFx(() -> new ElementViewFactory().create(element, PX_PER_MM));
    }

    private static Shape shapeOf(Node node) {
        Pane holder = assertInstanceOf(Pane.class, node);
        return assertInstanceOf(Shape.class, holder.getChildren().getFirst());
    }

    private static ShapeElement shape(ShapeType type) {
        return new ShapeElement(frame(), type, "#64748B", "#FFFFFF", 0.3);
    }

    private static TextElement textAligned(TextAlignment alignment) {
        return new TextElement(frame(), "x", "Arial", 10, "#000000", false, false, alignment);
    }

    private static ElementFrame frame() {
        return ElementFrame.create("element", BOUNDS, 0);
    }
}
