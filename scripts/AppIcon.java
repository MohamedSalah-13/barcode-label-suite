import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the application icon: a white barcode label, with its price line, on the blue tile of the app's primary
 * colour. Run from the project root after changing it:
 *
 * <pre>java scripts/AppIcon.java</pre>
 *
 * It writes the window icons the app loads, the Windows .ico jpackage uses and the README image. Small sizes are
 * drawn with fewer, pixel-aligned bars and no tilt, so they stay sharp in the taskbar and title bar.
 */
public final class AppIcon {
    private static final Color TILE_TOP = new Color(0x3B82F6);
    private static final Color TILE_BOTTOM = new Color(0x1E3A8A);
    private static final Color INK = new Color(0x0F172A);
    private static final Color PRICE = new Color(0xF59E0B);
    private static final Color MUTED = new Color(0xCBD5E1);
    private static final int[] WINDOW_SIZES = {16, 24, 32, 48, 64, 128, 256};
    /** Bar and gap widths, in modules, of the large icon's barcode: bar, gap, bar, gap... */
    private static final int[] PATTERN = {3, 1, 1, 2, 2, 1, 1, 1, 3, 2, 1, 1, 2, 1, 1, 2, 3, 1, 1, 1, 2};

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        Path icons = root.resolve("barcode-label-app/src/main/resources/com/mohamed/barcodelabel/app/icons");
        Files.createDirectories(icons);
        List<byte[]> pngs = new ArrayList<>();
        for (int size : WINDOW_SIZES) {
            byte[] png = png(draw(size));
            Files.write(icons.resolve("app-" + size + ".png"), png);
            pngs.add(png);
        }
        Path packaging = root.resolve("barcode-label-app/src/main/package");
        Files.createDirectories(packaging);
        Files.write(packaging.resolve("BarcodeLabelSuite.ico"), ico(WINDOW_SIZES, pngs));
        Files.write(packaging.resolve("BarcodeLabelSuite.png"), png(draw(512)));
        Files.write(root.resolve("docs/images/app-icon.png"), png(draw(256)));
        System.out.println("Icons written under " + root.toAbsolutePath().normalize());
    }

    static BufferedImage draw(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (size <= 48) drawSmall(g, size); else drawLarge(g, size);
        g.dispose();
        return image;
    }

    /** The full icon, drawn on a 256-unit grid: a tilted label with a shadow, a punched hole and a long barcode. */
    private static void drawLarge(Graphics2D g, int size) {
        double s = size / 256.0;
        g.scale(s, s);
        g.setPaint(new GradientPaint(0, 8, TILE_TOP, 0, 248, TILE_BOTTOM));
        g.fill(new RoundRectangle2D.Double(8, 8, 240, 240, 60, 60));
        // A soft highlight that fades out down the tile.
        g.setPaint(new GradientPaint(0, 8, new Color(255, 255, 255, 40), 0, 150, new Color(255, 255, 255, 0)));
        g.fill(new RoundRectangle2D.Double(8, 8, 240, 240, 60, 60));

        AffineTransform tile = g.getTransform();
        g.rotate(Math.toRadians(-8), 128, 132);
        RoundRectangle2D label = new RoundRectangle2D.Double(38, 66, 180, 132, 22, 22);
        g.setColor(new Color(0, 0, 0, 60));
        g.translate(4, 9);
        g.fill(label);
        g.translate(-4, -9);
        g.setColor(Color.WHITE);
        g.fill(label);
        // The hole of a hanging price tag, at the label's top left.
        g.setColor(TILE_BOTTOM);
        g.fill(new Ellipse2D.Double(52, 78, 13, 13));

        int modules = 0;
        for (int width : PATTERN) modules += width;
        double left = 74, right = 202, module = (right - left) / modules, x = left;
        g.setColor(INK);
        for (int i = 0; i < PATTERN.length; i++) {
            double width = PATTERN[i] * module;
            if (i % 2 == 0) g.fill(new java.awt.geom.Rectangle2D.Double(x, 86, width, 66));
            x += width;
        }
        g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(PRICE);
        g.drawLine(74, 172, 140, 172);
        g.setColor(MUTED);
        g.drawLine(158, 172, 198, 172);
        g.setTransform(tile);
    }

    /** Small-icon barcodes, bar first, from the shortest; each size takes the longest that fits its label. */
    private static final int[][] SMALL_PATTERNS = {
            {2, 1, 1, 1, 1, 1, 2},
            {2, 1, 1, 1, 2, 1, 1, 2, 1, 1, 2},
            {2, 1, 1, 2, 1, 1, 2, 1, 1, 1, 2, 2, 1, 1, 2},
            {2, 1, 1, 2, 1, 1, 2, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 1, 2}};

    /** The taskbar and title-bar sizes: an upright label whose bars sit on whole pixels and always fit it. */
    private static void drawSmall(Graphics2D g, int size) {
        g.setPaint(new GradientPaint(0, 0, TILE_TOP, 0, size, TILE_BOTTOM));
        double arc = size * 0.3;
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, arc, arc));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int margin = Math.max(2, Math.round(size * 0.14f));
        int top = Math.round(size * 0.22f), bottom = size - Math.round(size * 0.2f);
        g.setColor(Color.WHITE);
        g.fillRect(margin, top, size - 2 * margin, bottom - top);

        int pad = Math.max(1, Math.round(size / 16f));
        int room = size - 2 * margin - 2 * pad;
        int[] bars = SMALL_PATTERNS[0];
        for (int[] pattern : SMALL_PATTERNS) if (modules(pattern) <= room) bars = pattern;
        int unit = Math.max(1, room / modules(bars));
        int width = modules(bars) * unit;
        int left = (size - width) / 2;
        int barBottom = bottom - pad;
        if (size > 16) {
            int line = Math.max(1, Math.round(size / 24f));
            int lineTop = bottom - pad - line;
            g.setColor(PRICE);
            g.fillRect(left, lineTop, width / 2, line);
            barBottom = lineTop - pad;
        }
        g.setColor(INK);
        int x = left;
        for (int i = 0; i < bars.length; i++) {
            if (i % 2 == 0) g.fillRect(x, top + pad, bars[i] * unit, barBottom - top - pad);
            x += bars[i] * unit;
        }
    }

    private static int modules(int[] pattern) {
        int total = 0;
        for (int width : pattern) total += width;
        return total;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** A Windows icon holding each size as a PNG, which Windows Vista and later read. */
    private static byte[] ico(int[] sizes, List<byte[]> pngs) {
        int offset = 6 + 16 * sizes.length;
        int total = offset;
        for (byte[] png : pngs) total += png.length;
        ByteBuffer ico = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            byte side = (byte) (sizes[i] >= 256 ? 0 : sizes[i]);
            ico.put(side).put(side).put((byte) 0).put((byte) 0);
            ico.putShort((short) 1).putShort((short) 32);
            ico.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        for (byte[] png : pngs) ico.put(png);
        return ico.array();
    }
}
