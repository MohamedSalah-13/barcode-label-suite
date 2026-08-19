package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.data.MissingFieldPolicy;
import com.mohamed.barcodelabel.data.TemplateFieldResolver;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.render.LabelRenderer;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.render.RenderRequest;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.text.AttributedString;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import static java.awt.font.TextAttribute.FAMILY;
import static java.awt.font.TextAttribute.POSTURE;
import static java.awt.font.TextAttribute.POSTURE_OBLIQUE;
import static java.awt.font.TextAttribute.SIZE;
import static java.awt.font.TextAttribute.WEIGHT;
import static java.awt.font.TextAttribute.WEIGHT_BOLD;

public final class BufferedImageLabelRenderer implements LabelRenderer<BufferedImage> {
    private final BarcodeRasterizer barcodeRasterizer;
    private final TemplateFieldResolver fieldResolver;

    public BufferedImageLabelRenderer() {
        this(CompositeBarcodeRasterizer.standard(), new TemplateFieldResolver());
    }

    public BufferedImageLabelRenderer(BarcodeRasterizer barcodeRasterizer, TemplateFieldResolver fieldResolver) {
        this.barcodeRasterizer = Objects.requireNonNull(barcodeRasterizer, "barcodeRasterizer");
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
    }

    @Override
    public BufferedImage render(RenderRequest request) throws RenderException {
        Objects.requireNonNull(request, "request");
        LabelTemplate template = resolveTemplate(request.template(), request.data());
        var labelSize = template.page().labelSize();
        int width = positivePixels(labelSize.width(), request.dpi());
        int height = positivePixels(labelSize.height(), request.dpi());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            configure(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            template.elements().stream()
                    .filter(element -> element.frame().visible())
                    .sorted(Comparator.comparingInt(element -> element.frame().zIndex()))
                    .forEach(element -> renderUnchecked(graphics, element, request.dpi()));
        } catch (UncheckedRenderException exception) {
            throw exception.cause;
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private LabelTemplate resolveTemplate(LabelTemplate template, Map<String, ?> data) {
        return data.isEmpty() ? template : fieldResolver.resolve(template, data, MissingFieldPolicy.FAIL);
    }

    private void renderUnchecked(Graphics2D graphics, LabelElement element, double dpi) {
        try {
            renderElement(graphics, element, dpi);
        } catch (RenderException exception) {
            throw new UncheckedRenderException(exception);
        }
    }

    private void renderElement(Graphics2D parent, LabelElement element, double dpi) throws RenderException {
        RectMm boundsMm = element.frame().bounds();
        double x = UnitConversions.mmToPixels(boundsMm.x(), dpi);
        double y = UnitConversions.mmToPixels(boundsMm.y(), dpi);
        double width = UnitConversions.mmToPixels(boundsMm.width(), dpi);
        double height = UnitConversions.mmToPixels(boundsMm.height(), dpi);

        Graphics2D graphics = (Graphics2D) parent.create();
        try {
            graphics.clip(new Rectangle2D.Double(x, y, width, height));
            if (element.frame().rotationDegrees() != 0) {
                graphics.rotate(Math.toRadians(element.frame().rotationDegrees()), x + width / 2, y + height / 2);
            }
            switch (element) {
                case TextElement text -> drawText(graphics, text, x, y, width, height, dpi);
                case BarcodeElement barcode -> drawBarcode(graphics, barcode, x, y, width, height, dpi);
                case ShapeElement shape -> drawShape(graphics, shape, x, y, width, height, dpi);
                case ImageElement image -> drawImage(graphics, image, x, y, width, height);
            }
        } finally {
            graphics.dispose();
        }
    }

    private void drawText(Graphics2D graphics, TextElement element, double x, double y,
                          double width, double height, double dpi) {
        AttributedString attributed = new AttributedString(element.textTemplate());
        attributed.addAttribute(FAMILY, element.fontFamily());
        attributed.addAttribute(SIZE, (float) (element.fontSizePoints() * dpi / 72.0));
        if (element.bold()) attributed.addAttribute(WEIGHT, WEIGHT_BOLD);
        if (element.italic()) attributed.addAttribute(POSTURE, POSTURE_OBLIQUE);

        TextLayout layout = new TextLayout(attributed.getIterator(), graphics.getFontRenderContext());
        float drawX = switch (element.alignment()) {
            case LEFT -> (float) x;
            case CENTER -> (float) (x + (width - layout.getAdvance()) / 2);
            case RIGHT -> (float) (x + width - layout.getAdvance());
        };
        float drawY = (float) (y + Math.max(layout.getAscent(), (height + layout.getAscent() - layout.getDescent()) / 2));
        graphics.setColor(parseColor(element.color()));
        layout.draw(graphics, drawX, drawY);
    }

    private void drawBarcode(Graphics2D graphics, BarcodeElement element, double x, double y,
                             double width, double height, double dpi) throws RenderException {
        int availableWidth = Math.max(1, (int) Math.floor(width));
        int availableHeight = Math.max(1, (int) Math.floor(height));
        BufferedImage barcode = barcodeRasterizer.rasterize(element, availableWidth, availableHeight, dpi);
        int drawX = (int) Math.round(x + (width - barcode.getWidth()) / 2);
        int drawY = (int) Math.round(y + (height - barcode.getHeight()) / 2);
        Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(barcode, drawX, drawY, null);
        if (oldInterpolation != null) graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
    }

    private void drawShape(Graphics2D graphics, ShapeElement element, double x, double y,
                           double width, double height, double dpi) {
        double strokeWidth = UnitConversions.mmToPixels(element.strokeWidthMm(), dpi);
        graphics.setStroke(new BasicStroke((float) Math.max(strokeWidth, 0.1)));
        java.awt.Shape shape = switch (element.shapeType()) {
            case LINE -> new java.awt.geom.Line2D.Double(x, y, x + width, y + height);
            case RECTANGLE -> new Rectangle2D.Double(x, y, width, height);
            case ROUNDED_RECTANGLE -> new RoundRectangle2D.Double(x, y, width, height,
                    Math.min(width, height) * 0.15, Math.min(width, height) * 0.15);
            case ELLIPSE -> new Ellipse2D.Double(x, y, width, height);
        };
        Color fill = parseColor(element.fillColor());
        if (fill.getAlpha() > 0 && element.shapeType() != ShapeType.LINE) {
            graphics.setColor(fill);
            graphics.fill(shape);
        }
        graphics.setColor(parseColor(element.strokeColor()));
        graphics.draw(shape);
    }

    private void drawImage(Graphics2D graphics, ImageElement element, double x, double y,
                           double width, double height) throws RenderException {
        BufferedImage source = readImage(element.source());
        double scaleX = width / source.getWidth();
        double scaleY = height / source.getHeight();
        double drawWidth;
        double drawHeight;
        if (element.fit() == ImageFit.STRETCH) {
            drawWidth = width;
            drawHeight = height;
        } else {
            double scale = element.fit() == ImageFit.CONTAIN
                    ? Math.min(scaleX, scaleY)
                    : Math.max(scaleX, scaleY);
            drawWidth = source.getWidth() * scale;
            drawHeight = source.getHeight() * scale;
        }
        int drawX = (int) Math.round(x + (width - drawWidth) / 2);
        int drawY = (int) Math.round(y + (height - drawHeight) / 2);
        graphics.drawImage(source, drawX, drawY, (int) Math.round(drawWidth), (int) Math.round(drawHeight), null);
    }

    private static BufferedImage readImage(String source) throws RenderException {
        try {
            BufferedImage image;
            if (source.startsWith("file:")) {
                image = ImageIO.read(Path.of(URI.create(source)).toFile());
            } else {
                image = ImageIO.read(new File(source));
            }
            if (image == null) throw new IOException("unsupported or empty image");
            return image;
        } catch (IOException | IllegalArgumentException exception) {
            throw new RenderException("Cannot read image: " + source, exception);
        }
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static int positivePixels(double millimetres, double dpi) {
        return Math.max(1, (int) Math.ceil(UnitConversions.mmToPixels(millimetres, dpi)));
    }

    static Color parseColor(String value) {
        if (value == null || value.isBlank() || "transparent".equalsIgnoreCase(value)) {
            return new Color(0, 0, 0, 0);
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            return switch (hex.length()) {
                case 6 -> new Color(Integer.parseInt(hex, 16));
                case 8 -> new Color((int) Long.parseLong(hex, 16), true);
                default -> throw new IllegalArgumentException("expected #RRGGBB or #AARRGGBB");
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid color: " + value, exception);
        }
    }

    private static final class UncheckedRenderException extends RuntimeException {
        private final RenderException cause;

        private UncheckedRenderException(RenderException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}

