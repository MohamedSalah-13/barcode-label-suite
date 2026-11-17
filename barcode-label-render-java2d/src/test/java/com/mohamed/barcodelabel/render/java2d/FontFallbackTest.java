package com.mohamed.barcodelabel.render.java2d;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FontFallbackTest {
    static final String ARABIC = "سعر المنتج";

    @Test
    void textTheFontCanDisplayKeepsItsFont() {
        String family = latinOnlyFont();
        List<Run> runs = runs(FontFallback.style("Price 25", style(family)));
        assertEquals(List.of(new Run("Price 25", family)), runs);
    }

    @Test
    void charactersTheFontCannotDisplayGetAFontThatCan() {
        String family = latinOnlyFont();
        String text = "Price " + ARABIC + " 25";
        List<Run> runs = runs(FontFallback.style(text, style(family)));

        for (Run run : runs) {
            Font font = new Font(Map.of(TextAttribute.FAMILY, run.family(), TextAttribute.SIZE, 12f));
            assertEquals(-1, font.canDisplayUpTo(run.text()), run + " is drawn in a font that has its glyphs");
        }
        assertEquals(new Run("Price ", family), runs.getFirst(), "Latin text keeps the chosen font");
        assertEquals(new Run(" 25", family), runs.getLast());
        assertTrue(runs.contains(new Run("سعر", Font.DIALOG)), "a word is one run, so it is shaped as a whole: " + runs);
    }

    /** An installed font with Latin glyphs but no Arabic ones, such as Liberation Sans or Georgia. */
    static String latinOnlyFont() {
        Optional<String> family = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames())
                .filter(name -> !isLogical(name))
                .filter(name -> {
                    Font font = new Font(name, Font.PLAIN, 12);
                    return font.getFamily().equals(name) && font.canDisplayUpTo("Price 25") == -1
                            && font.canDisplayUpTo(ARABIC.replace(" ", "")) == 0;
                })
                .sorted()
                .findFirst();
        assumeTrue(family.isPresent(), "no installed font lacks Arabic glyphs");
        return family.get();
    }

    private static boolean isLogical(String family) {
        return List.of(Font.DIALOG, Font.DIALOG_INPUT, Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED).contains(family);
    }

    private static Map<TextAttribute, Object> style(String family) {
        return Map.of(TextAttribute.FAMILY, family, TextAttribute.SIZE, 12f);
    }

    private record Run(String text, Object family) {}

    private static List<Run> runs(java.text.AttributedString styled) {
        AttributedCharacterIterator iterator = styled.getIterator();
        List<Run> runs = new ArrayList<>();
        while (iterator.getIndex() < iterator.getEndIndex()) {
            int limit = iterator.getRunLimit(TextAttribute.FAMILY);
            StringBuilder text = new StringBuilder();
            Object family = iterator.getAttribute(TextAttribute.FAMILY);
            for (char c = iterator.current(); iterator.getIndex() < limit; c = iterator.next()) text.append(c);
            runs.add(new Run(text.toString(), family));
        }
        return runs;
    }
}
