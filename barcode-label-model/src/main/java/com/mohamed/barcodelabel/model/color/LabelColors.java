package com.mohamed.barcodelabel.model.color;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Turns the colour strings stored in a template into ARGB values, the same way for the designer and for
 * printing, so a label prints in the colours it shows.
 *
 * <p>The accepted forms are those of JavaFX's {@code Color.web}, which the designer used before: the 148
 * named colours ({@code red}, {@code transparent}, ...), {@code #RGB}, {@code #RGBA}, {@code #RRGGBB} and
 * {@code #RRGGBBAA} (alpha last), the same with a {@code 0x} prefix or no prefix, {@code rgb(r, g, b)} and
 * {@code rgba(r, g, b, a)} with 0-255 integers or percentages and an alpha from 0 to 1, and
 * {@code hsl(h, s%, l%)} / {@code hsla(...)}. Like JavaFX, {@code hsl} is computed as hue, saturation and
 * brightness. Case does not matter; spaces are allowed only inside the parentheses.</p>
 */
public final class LabelColors {
    /** Fully transparent black: what a missing colour means. */
    public static final int TRANSPARENT = 0x00000000;
    public static final int BLACK = 0xFF000000;

    private static final Map<String, Integer> NAMED = Map.ofEntries(
            Map.entry("aliceblue", 0xfff0f8ff),
            Map.entry("antiquewhite", 0xfffaebd7),
            Map.entry("aqua", 0xff00ffff),
            Map.entry("aquamarine", 0xff7fffd4),
            Map.entry("azure", 0xfff0ffff),
            Map.entry("beige", 0xfff5f5dc),
            Map.entry("bisque", 0xffffe4c4),
            Map.entry("black", 0xff000000),
            Map.entry("blanchedalmond", 0xffffebcd),
            Map.entry("blue", 0xff0000ff),
            Map.entry("blueviolet", 0xff8a2be2),
            Map.entry("brown", 0xffa52a2a),
            Map.entry("burlywood", 0xffdeb887),
            Map.entry("cadetblue", 0xff5f9ea0),
            Map.entry("chartreuse", 0xff7fff00),
            Map.entry("chocolate", 0xffd2691e),
            Map.entry("coral", 0xffff7f50),
            Map.entry("cornflowerblue", 0xff6495ed),
            Map.entry("cornsilk", 0xfffff8dc),
            Map.entry("crimson", 0xffdc143c),
            Map.entry("cyan", 0xff00ffff),
            Map.entry("darkblue", 0xff00008b),
            Map.entry("darkcyan", 0xff008b8b),
            Map.entry("darkgoldenrod", 0xffb8860b),
            Map.entry("darkgray", 0xffa9a9a9),
            Map.entry("darkgreen", 0xff006400),
            Map.entry("darkgrey", 0xffa9a9a9),
            Map.entry("darkkhaki", 0xffbdb76b),
            Map.entry("darkmagenta", 0xff8b008b),
            Map.entry("darkolivegreen", 0xff556b2f),
            Map.entry("darkorange", 0xffff8c00),
            Map.entry("darkorchid", 0xff9932cc),
            Map.entry("darkred", 0xff8b0000),
            Map.entry("darksalmon", 0xffe9967a),
            Map.entry("darkseagreen", 0xff8fbc8f),
            Map.entry("darkslateblue", 0xff483d8b),
            Map.entry("darkslategray", 0xff2f4f4f),
            Map.entry("darkslategrey", 0xff2f4f4f),
            Map.entry("darkturquoise", 0xff00ced1),
            Map.entry("darkviolet", 0xff9400d3),
            Map.entry("deeppink", 0xffff1493),
            Map.entry("deepskyblue", 0xff00bfff),
            Map.entry("dimgray", 0xff696969),
            Map.entry("dimgrey", 0xff696969),
            Map.entry("dodgerblue", 0xff1e90ff),
            Map.entry("firebrick", 0xffb22222),
            Map.entry("floralwhite", 0xfffffaf0),
            Map.entry("forestgreen", 0xff228b22),
            Map.entry("fuchsia", 0xffff00ff),
            Map.entry("gainsboro", 0xffdcdcdc),
            Map.entry("ghostwhite", 0xfff8f8ff),
            Map.entry("gold", 0xffffd700),
            Map.entry("goldenrod", 0xffdaa520),
            Map.entry("gray", 0xff808080),
            Map.entry("green", 0xff008000),
            Map.entry("greenyellow", 0xffadff2f),
            Map.entry("grey", 0xff808080),
            Map.entry("honeydew", 0xfff0fff0),
            Map.entry("hotpink", 0xffff69b4),
            Map.entry("indianred", 0xffcd5c5c),
            Map.entry("indigo", 0xff4b0082),
            Map.entry("ivory", 0xfffffff0),
            Map.entry("khaki", 0xfff0e68c),
            Map.entry("lavender", 0xffe6e6fa),
            Map.entry("lavenderblush", 0xfffff0f5),
            Map.entry("lawngreen", 0xff7cfc00),
            Map.entry("lemonchiffon", 0xfffffacd),
            Map.entry("lightblue", 0xffadd8e6),
            Map.entry("lightcoral", 0xfff08080),
            Map.entry("lightcyan", 0xffe0ffff),
            Map.entry("lightgoldenrodyellow", 0xfffafad2),
            Map.entry("lightgray", 0xffd3d3d3),
            Map.entry("lightgreen", 0xff90ee90),
            Map.entry("lightgrey", 0xffd3d3d3),
            Map.entry("lightpink", 0xffffb6c1),
            Map.entry("lightsalmon", 0xffffa07a),
            Map.entry("lightseagreen", 0xff20b2aa),
            Map.entry("lightskyblue", 0xff87cefa),
            Map.entry("lightslategray", 0xff778899),
            Map.entry("lightslategrey", 0xff778899),
            Map.entry("lightsteelblue", 0xffb0c4de),
            Map.entry("lightyellow", 0xffffffe0),
            Map.entry("lime", 0xff00ff00),
            Map.entry("limegreen", 0xff32cd32),
            Map.entry("linen", 0xfffaf0e6),
            Map.entry("magenta", 0xffff00ff),
            Map.entry("maroon", 0xff800000),
            Map.entry("mediumaquamarine", 0xff66cdaa),
            Map.entry("mediumblue", 0xff0000cd),
            Map.entry("mediumorchid", 0xffba55d3),
            Map.entry("mediumpurple", 0xff9370db),
            Map.entry("mediumseagreen", 0xff3cb371),
            Map.entry("mediumslateblue", 0xff7b68ee),
            Map.entry("mediumspringgreen", 0xff00fa9a),
            Map.entry("mediumturquoise", 0xff48d1cc),
            Map.entry("mediumvioletred", 0xffc71585),
            Map.entry("midnightblue", 0xff191970),
            Map.entry("mintcream", 0xfff5fffa),
            Map.entry("mistyrose", 0xffffe4e1),
            Map.entry("moccasin", 0xffffe4b5),
            Map.entry("navajowhite", 0xffffdead),
            Map.entry("navy", 0xff000080),
            Map.entry("oldlace", 0xfffdf5e6),
            Map.entry("olive", 0xff808000),
            Map.entry("olivedrab", 0xff6b8e23),
            Map.entry("orange", 0xffffa500),
            Map.entry("orangered", 0xffff4500),
            Map.entry("orchid", 0xffda70d6),
            Map.entry("palegoldenrod", 0xffeee8aa),
            Map.entry("palegreen", 0xff98fb98),
            Map.entry("paleturquoise", 0xffafeeee),
            Map.entry("palevioletred", 0xffdb7093),
            Map.entry("papayawhip", 0xffffefd5),
            Map.entry("peachpuff", 0xffffdab9),
            Map.entry("peru", 0xffcd853f),
            Map.entry("pink", 0xffffc0cb),
            Map.entry("plum", 0xffdda0dd),
            Map.entry("powderblue", 0xffb0e0e6),
            Map.entry("purple", 0xff800080),
            Map.entry("red", 0xffff0000),
            Map.entry("rosybrown", 0xffbc8f8f),
            Map.entry("royalblue", 0xff4169e1),
            Map.entry("saddlebrown", 0xff8b4513),
            Map.entry("salmon", 0xfffa8072),
            Map.entry("sandybrown", 0xfff4a460),
            Map.entry("seagreen", 0xff2e8b57),
            Map.entry("seashell", 0xfffff5ee),
            Map.entry("sienna", 0xffa0522d),
            Map.entry("silver", 0xffc0c0c0),
            Map.entry("skyblue", 0xff87ceeb),
            Map.entry("slateblue", 0xff6a5acd),
            Map.entry("slategray", 0xff708090),
            Map.entry("slategrey", 0xff708090),
            Map.entry("snow", 0xfffffafa),
            Map.entry("springgreen", 0xff00ff7f),
            Map.entry("steelblue", 0xff4682b4),
            Map.entry("tan", 0xffd2b48c),
            Map.entry("teal", 0xff008080),
            Map.entry("thistle", 0xffd8bfd8),
            Map.entry("tomato", 0xffff6347),
            Map.entry("transparent", 0x00000000),
            Map.entry("turquoise", 0xff40e0d0),
            Map.entry("violet", 0xffee82ee),
            Map.entry("wheat", 0xfff5deb3),
            Map.entry("white", 0xffffffff),
            Map.entry("whitesmoke", 0xfff5f5f5),
            Map.entry("yellow", 0xffffff00),
            Map.entry("yellowgreen", 0xff9acd32));

    private LabelColors() {}

    /**
     * The colour as ARGB. {@code null} or blank means transparent.
     *
     * @throws IllegalArgumentException if the text is not a colour
     */
    public static int toArgb(String value) {
        if (value == null || value.isBlank()) return TRANSPARENT;
        String color = value.toLowerCase(Locale.ROOT);
        try {
            if (color.startsWith("#")) return hex(color.substring(1), value);
            if (color.startsWith("0x")) return hex(color.substring(2), value);
            if (color.startsWith("rgba(")) return rgb(color, 5, true, value);
            if (color.startsWith("rgb(")) return rgb(color, 4, false, value);
            if (color.startsWith("hsla(")) return hsb(color, 5, true, value);
            if (color.startsWith("hsl(")) return hsb(color, 4, false, value);
            Integer named = NAMED.get(color);
            if (named != null) return named;
            return hex(color, value);
        } catch (NumberFormatException exception) {
            throw invalid(value);
        }
    }

    /** Every colour name that is accepted, in lower case. */
    public static Set<String> names() {
        return NAMED.keySet();
    }

    /** The colour as ARGB, or black when the text is not a colour, as the designer has always shown it. */
    public static int toArgbOrBlack(String value) {
        try {
            return toArgb(value);
        } catch (IllegalArgumentException exception) {
            return BLACK;
        }
    }

    private static int hex(String digits, String original) {
        return switch (digits.length()) {
            case 3 -> argb(nibble(digits, 0), nibble(digits, 1), nibble(digits, 2), 255);
            case 4 -> argb(nibble(digits, 0), nibble(digits, 1), nibble(digits, 2), nibble(digits, 3));
            case 6 -> argb(octet(digits, 0), octet(digits, 2), octet(digits, 4), 255);
            case 8 -> argb(octet(digits, 0), octet(digits, 2), octet(digits, 4), octet(digits, 6));
            default -> throw invalid(original);
        };
    }

    private static int nibble(String digits, int index) {
        return Integer.parseInt(digits.substring(index, index + 1), 16) * 17;
    }

    private static int octet(String digits, int index) {
        return Integer.parseInt(digits.substring(index, index + 2), 16);
    }

    private static int rgb(String color, int start, boolean hasAlpha, String original) {
        String[] parts = arguments(color, start, hasAlpha ? 4 : 3, original);
        double red = component(parts[0]);
        double green = component(parts[1]);
        double blue = component(parts[2]);
        double alpha = hasAlpha ? alpha(parts[3]) : 1;
        return argb(red, green, blue, alpha);
    }

    private static int hsb(String color, int start, boolean hasAlpha, String original) {
        String[] parts = arguments(color, start, hasAlpha ? 4 : 3, original);
        double hue = Double.parseDouble(parts[0].trim());
        double saturation = percentage(parts[1], original);
        double brightness = percentage(parts[2], original);
        double alpha = hasAlpha ? alpha(parts[3]) : 1;
        double[] rgb = hsbToRgb(hue, saturation, brightness);
        return argb(rgb[0], rgb[1], rgb[2], alpha);
    }

    private static String[] arguments(String color, int start, int count, String original) {
        if (!color.endsWith(")")) throw invalid(original);
        String[] parts = color.substring(start, color.length() - 1).split(",", -1);
        if (parts.length != count) throw invalid(original);
        return parts;
    }

    /** A 0-255 integer or a percentage, as a fraction from 0 to 1. */
    private static double component(String text) {
        String value = text.trim();
        if (value.endsWith("%")) return clamp(Double.parseDouble(value.substring(0, value.length() - 1)) / 100);
        return clamp(Integer.parseInt(value) / 255.0);
    }

    private static double percentage(String text, String original) {
        String value = text.trim();
        if (!value.endsWith("%")) throw invalid(original);
        return clamp(Double.parseDouble(value.substring(0, value.length() - 1)) / 100);
    }

    private static double alpha(String text) {
        return clamp(Double.parseDouble(text.trim()));
    }

    /** Hue in degrees, saturation and brightness from 0 to 1, as JavaFX converts them. */
    private static double[] hsbToRgb(double hue, double saturation, double brightness) {
        double normalized = ((hue % 360) + 360) % 360 / 360;
        if (saturation == 0) return new double[] {brightness, brightness, brightness};
        double sector = (normalized - Math.floor(normalized)) * 6;
        double fraction = sector - Math.floor(sector);
        double p = brightness * (1 - saturation);
        double q = brightness * (1 - saturation * fraction);
        double t = brightness * (1 - saturation * (1 - fraction));
        return switch ((int) sector) {
            case 0 -> new double[] {brightness, t, p};
            case 1 -> new double[] {q, brightness, p};
            case 2 -> new double[] {p, brightness, t};
            case 3 -> new double[] {p, q, brightness};
            case 4 -> new double[] {t, p, brightness};
            default -> new double[] {brightness, p, q};
        };
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** Components from 0 to 1 as 8-bit channels, held as floats first, as JavaFX stores them, to round alike. */
    private static int argb(double red, double green, double blue, double alpha) {
        return argb(channel(red), channel(green), channel(blue), channel(alpha));
    }

    private static int channel(double fraction) {
        return (int) Math.round((double) (float) fraction * 255);
    }

    private static int argb(int red, int green, int blue, int alpha) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Invalid color: " + Objects.toString(value));
    }
}
