package com.mohamed.barcodelabel.render.java2d;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

/**
 * Styles text so that characters the chosen font has no glyphs for are drawn in a font that has them.
 *
 * <p>A physical font such as "Liberation Sans" has no Arabic glyphs, and Java2D draws a box for each character it
 * cannot display. JavaFX, which draws the designer canvas, substitutes another font instead, so without this the
 * designer would show text that prints as boxes. Those characters are given the {@link Font#DIALOG} logical font,
 * a composite of the system's fonts that covers most scripts.
 */
final class FontFallback {
    private FontFallback() {}

    static AttributedString style(String text, Map<TextAttribute, ?> style) {
        AttributedString styled = new AttributedString(text, style);
        Font requested = Font.getFont(style);
        if (requested.canDisplayUpTo(text) == -1) return styled;

        Map<TextAttribute, Object> fallbackStyle = new HashMap<>(style);
        fallbackStyle.put(TextAttribute.FAMILY, Font.DIALOG);
        Font fallback = Font.getFont(fallbackStyle);
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            if (!requested.canDisplay(codePoint) && fallback.canDisplay(codePoint)) {
                // Adjacent characters with the same family form one run, so a word is still shaped as a whole.
                styled.addAttribute(TextAttribute.FAMILY, Font.DIALOG, index, next);
            }
            index = next;
        }
        return styled;
    }
}
