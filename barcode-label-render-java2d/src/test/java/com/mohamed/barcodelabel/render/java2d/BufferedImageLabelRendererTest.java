package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.render.RenderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renders at 100 dpi on a 50.8 x 25.4 mm label, so 1 mm is 100 / 25.4 px and the image is 200 x 100 px. */
class BufferedImageLabelRendererTest {
    private static final double DPI = 100;
    private static final int WHITE = 0xFFFFFFFF;
    private final BufferedImageLabelRenderer renderer = new BufferedImageLabelRenderer();

    @Test
    void theImageHasTheLabelsSizeAndAWhiteBackground() throws RenderException {
        BufferedImage image = render(List.of());
        assertEquals(200, image.getWidth());
        assertEquals(100, image.getHeight());
        assertEquals(WHITE, image.getRGB(0, 0));
        assertEquals(WHITE, image.getRGB(199, 99));
    }

    @Test
    void shapesAreFilledAndStrokedInsideTheirFrame() throws RenderException {
        // A red-filled, blue-stroked rectangle over the left half (0..25.4 mm = 0..100 px).
        BufferedImage image = render(List.of(new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 0),
                ShapeType.RECTANGLE, "#0000FF", "#FF0000", 1)));
        assertEquals(0xFFFF0000, image.getRGB(50, 50), "filled inside");
        assertEquals(0xFF0000FF, image.getRGB(1, 50), "stroked at the edge");
        assertEquals(WHITE, image.getRGB(150, 50), "nothing outside the frame");
    }

    @Test
    void aTransparentFillLeavesTheInsideEmptyAndALineDrawsItsDiagonal() throws RenderException {
        BufferedImage outline = render(List.of(new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 0),
                ShapeType.ELLIPSE, "#000000", "transparent", 0.5)));
        assertEquals(WHITE, outline.getRGB(50, 50));
        assertTrue(dark(outline, 0, 0, 100, 100) > 0, "the ellipse outline is drawn");

        BufferedImage line = render(List.of(new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 0),
                ShapeType.LINE, "#000000", "#FF0000", 0.5)));
        assertTrue(isDark(line.getRGB(50, 50)), "the diagonal passes through the centre");
        assertEquals(WHITE, line.getRGB(90, 10), "a line is never filled");
    }

    @Test
    void roundedRectanglesHaveRoundedCorners() throws RenderException {
        BufferedImage image = render(List.of(new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 0),
                ShapeType.ROUNDED_RECTANGLE, "#FF0000", "#FF0000", 0.1)));
        assertEquals(0xFFFF0000, image.getRGB(50, 0), "the top edge is drawn");
        assertEquals(WHITE, image.getRGB(0, 0), "the corner is cut");
    }

    @Test
    void higherLayersAreDrawnOnTopAndHiddenElementsNotAtAll() throws RenderException {
        LabelElement red = new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 1), ShapeType.RECTANGLE,
                "#FF0000", "#FF0000", 0.1);
        LabelElement green = new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 2), ShapeType.RECTANGLE,
                "#00FF00", "#00FF00", 0.1);
        // Listed top layer first: drawing order must follow the z index, not the list order.
        assertEquals(0xFF00FF00, render(List.of(green, red)).getRGB(50, 50));

        ElementFrame hiddenFrame = new ElementFrame(java.util.UUID.randomUUID(), "hidden",
                new RectMm(0, 0, 25.4, 25.4), 0, 3, false, false);
        LabelElement hidden = new ShapeElement(hiddenFrame, ShapeType.RECTANGLE, "#0000FF", "#0000FF", 0.1);
        assertEquals(0xFF00FF00, render(List.of(green, red, hidden)).getRGB(50, 50));
    }

    @Test
    void textIsDrawnInItsColourAndAligned() throws RenderException {
        LabelElement left = new TextElement(frame(new RectMm(0, 0, 50.8, 12.7), 0), "Label", "Dialog", 20,
                "#FF0000", true, true, TextAlignment.LEFT);
        BufferedImage image = render(List.of(left));
        assertTrue(dark(image, 0, 0, 100, 50) > 0, "left-aligned text starts at the left");
        assertEquals(0, dark(image, 150, 0, 200, 50), "and does not reach the right edge");
        assertTrue(reddish(image, 0, 0, 100, 50), "in its colour");

        LabelElement right = new TextElement(frame(new RectMm(0, 0, 50.8, 12.7), 0), "Label", "Dialog", 20,
                "#000000", false, false, TextAlignment.RIGHT);
        BufferedImage rightImage = render(List.of(right));
        assertEquals(0, dark(rightImage, 0, 0, 50, 50));
        assertTrue(dark(rightImage, 100, 0, 200, 50) > 0);

        LabelElement center = new TextElement(frame(new RectMm(0, 0, 50.8, 12.7), 0), "Label", "Dialog", 20,
                "#000000", false, false, TextAlignment.CENTER);
        BufferedImage centerImage = render(List.of(center));
        assertEquals(0, dark(centerImage, 0, 0, 30, 50));
        assertEquals(0, dark(centerImage, 170, 0, 200, 50));
        assertTrue(dark(centerImage, 80, 0, 120, 50) > 0);
    }

    @Test
    void arabicTextInAFontWithoutArabicGlyphsPrintsInAFallbackFont() throws RenderException {
        String latinOnly = FontFallbackTest.latinOnlyFont();
        BufferedImage inLatinOnlyFont = render(List.of(arabicWord(latinOnly)));
        BufferedImage inDialog = render(List.of(arabicWord("Dialog")));

        assertTrue(dark(inDialog, 0, 0, 200, 100) > 0, "the word is drawn");
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 100; y++) {
                assertEquals(inDialog.getRGB(x, y), inLatinOnlyFont.getRGB(x, y),
                        latinOnly + " draws the word as Dialog does, not as missing-glyph boxes, at " + x + "," + y);
            }
        }
    }

    private static LabelElement arabicWord(String fontFamily) {
        return new TextElement(frame(new RectMm(0, 0, 50.8, 25.4), 0), "المنتج", fontFamily, 20,
                "#000000", false, false, TextAlignment.CENTER);
    }

    @Test
    void fieldsAreFilledFromTheDataAndAMissingFieldFails() throws RenderException {
        TextElement name = new TextElement(frame(new RectMm(0, 0, 50.8, 12.7), 0), "{{name}}", "Dialog", 20,
                "#000000", false, false, TextAlignment.LEFT);
        BufferedImage filled = renderer.render(new RenderRequest(template(List.of(name)), Map.of("name", "WWWW"), DPI));
        BufferedImage shorter = renderer.render(new RenderRequest(template(List.of(name)), Map.of("name", "i"), DPI));
        assertTrue(dark(filled, 0, 0, 200, 50) > dark(shorter, 0, 0, 200, 50), "the data's text is drawn");

        assertThrows(RuntimeException.class, () -> renderer.render(
                new RenderRequest(template(List.of(name)), Map.of("other", "x"), DPI)));
    }

    @Test
    void barcodesAreCentredInTheirFrame() throws RenderException {
        LabelElement barcode = new BarcodeElement(frame(new RectMm(0, 0, 50.8, 25.4), 0), "12345",
                Symbology.CODE_128, 0.25, 2, false);
        BufferedImage image = render(List.of(barcode));
        int left = dark(image, 0, 0, 100, 100);
        int right = dark(image, 100, 0, 200, 100);
        assertTrue(left > 0 && right > 0, "bars on both halves");
        assertEquals(0, dark(image, 0, 0, 10, 100), "a quiet zone on the left");
    }

    @Test
    void aBarcodeThatDoesNotFitItsFrameIsARenderError() {
        LabelElement barcode = new BarcodeElement(frame(new RectMm(0, 0, 10, 10), 0), "ABCDEFGHIJKLMNOP",
                Symbology.CODE_128, 0.5, 2, true);
        assertThrows(RenderException.class, () -> render(List.of(barcode)));
    }

    @Test
    void rotationTurnsTheElementAroundItsCentre() throws RenderException {
        ElementFrame turned = new ElementFrame(java.util.UUID.randomUUID(), "bar", new RectMm(0, 0, 25.4, 25.4),
                90, 0, true, false);
        LabelElement diagonal = new ShapeElement(turned, ShapeType.LINE, "#000000", "transparent", 1);
        BufferedImage image = render(List.of(diagonal));
        // The diagonal from top-left to bottom-right, turned 90 degrees, runs from top-right to bottom-left.
        assertTrue(isDark(image.getRGB(85, 15)), "now crosses the top-right");
        assertEquals(WHITE, image.getRGB(15, 15), "and no longer the top-left");
    }

    @Test
    void rotatingAWideRectanglePreservesBothEndsOutsideItsOriginalFrame() throws RenderException {
        // At 100 dpi the frame spans x=60..140 and y=40..60, centred on (100, 50).
        var frame = new ElementFrame(java.util.UUID.randomUUID(), "bar", new RectMm(15.24, 10.16, 20.32, 5.08),
                90, 0, true, false);
        var bar = new ShapeElement(frame, ShapeType.RECTANGLE, "black", "black", 0.1);
        BufferedImage image = render(List.of(bar));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(100, 12));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(100, 88));
        assertEquals(WHITE, image.getRGB(62, 50));
        assertEquals(WHITE, image.getRGB(100, 92));
    }

    @Test
    void imagesAreScaledByTheirFit(@TempDir Path directory) throws Exception {
        // A 20 x 10 px red image in a square 100 x 100 px frame.
        BufferedImage source = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 20; x++) for (int y = 0; y < 10; y++) source.setRGB(x, y, 0xFF0000);
        Path file = directory.resolve("red.png");
        ImageIO.write(source, "png", file.toFile());
        RectMm square = new RectMm(0, 0, 25.4, 25.4);

        BufferedImage contain = render(List.of(new ImageElement(frame(square, 0), file.toString(), ImageFit.CONTAIN)));
        assertEquals(0xFFFF0000, contain.getRGB(50, 50));
        assertEquals(WHITE, contain.getRGB(50, 5), "CONTAIN leaves bands above and below");

        BufferedImage stretch = render(List.of(new ImageElement(frame(square, 0), file.toUri().toString(), ImageFit.STRETCH)));
        assertEquals(0xFFFF0000, stretch.getRGB(50, 5), "STRETCH fills the frame, from a file: URL too");

        BufferedImage cover = render(List.of(new ImageElement(frame(square, 0), file.toString(), ImageFit.COVER)));
        assertEquals(0xFFFF0000, cover.getRGB(50, 5));
        assertEquals(WHITE, cover.getRGB(150, 50), "COVER is clipped to its frame");
    }

    @Test
    void anUnreadableImageIsARenderError(@TempDir Path directory) {
        String missing = directory.resolve("missing.png").toString();
        RenderException error = assertThrows(RenderException.class, () -> render(List.of(
                new ImageElement(frame(new RectMm(0, 0, 10, 10), 0), missing, ImageFit.CONTAIN))));
        assertTrue(error.getMessage().contains(missing));
    }

    @Test
    void coloursAreReadAsTheDesignerReadsThem() {
        assertEquals(new Color(0x12, 0x34, 0x56), BufferedImageLabelRenderer.parseColor("#123456"));
        assertEquals(new Color(0x12, 0x34, 0x56), BufferedImageLabelRenderer.parseColor("123456"));
        assertEquals(new Color(0x12, 0x34, 0x56, 0x78), BufferedImageLabelRenderer.parseColor("#12345678"),
                "alpha comes last, as in the designer and CSS");
        assertEquals(Color.RED, BufferedImageLabelRenderer.parseColor("red"));
        assertEquals(new Color(0, 128, 0), BufferedImageLabelRenderer.parseColor("Green"));
        assertEquals(new Color(255, 0, 0, 128), BufferedImageLabelRenderer.parseColor("rgba(255, 0, 0, 0.5)"));
        assertEquals(0, BufferedImageLabelRenderer.parseColor("transparent").getAlpha());
        assertEquals(0, BufferedImageLabelRenderer.parseColor("").getAlpha());
        assertEquals(Color.BLACK, BufferedImageLabelRenderer.parseColor("#12"), "the designer shows black too");
        assertEquals(Color.BLACK, BufferedImageLabelRenderer.parseColor("not-a-color"));
    }

    @Test
    void aNamedColourPrintsInThatColour() throws RenderException {
        // Rendering used to fail on colour names, though the designer showed them.
        BufferedImage image = render(List.of(new ShapeElement(frame(new RectMm(0, 0, 25.4, 25.4), 0),
                ShapeType.RECTANGLE, "navy", "orange", 0.1)));
        assertEquals(0xFFFFA500, image.getRGB(50, 50));
    }

    private BufferedImage render(List<LabelElement> elements) throws RenderException {
        return renderer.render(new RenderRequest(template(elements), Map.of(), DPI));
    }

    private static LabelTemplate template(List<LabelElement> elements) {
        var layout = new SheetLayout(1, 1, new DimensionsMm(50.8, 25.4), 0, 0);
        return LabelTemplate.create("Renderer test", PageDefinition.a4Sheet(layout, new InsetsMm(10, 10, 10, 10)),
                elements);
    }

    private static ElementFrame frame(RectMm bounds, int z) {
        return ElementFrame.create("element", bounds, z);
    }

    private static boolean isDark(int argb) {
        Color color = new Color(argb, true);
        return color.getRed() + color.getGreen() + color.getBlue() < 3 * 200;
    }

    /** Pixels in [x0, x1) x [y0, y1) that are clearly not white. */
    private static int dark(BufferedImage image, int x0, int y0, int x1, int y1) {
        int count = 0;
        for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) if (isDark(image.getRGB(x, y))) count++;
        return count;
    }

    private static boolean reddish(BufferedImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                Color c = new Color(image.getRGB(x, y));
                if (c.getRed() > 150 && c.getGreen() < 80 && c.getBlue() < 80) return true;
            }
        }
        return false;
    }
}
