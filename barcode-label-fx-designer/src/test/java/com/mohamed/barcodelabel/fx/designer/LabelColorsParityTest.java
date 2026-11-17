package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.color.LabelColors;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LabelColors replaced JavaFX's Color.web in the designer and is what printing uses, so it must read every
 * colour exactly as Color.web does: the label must look the same before and after this change, and print as
 * it looks.
 */
class LabelColorsParityTest {
    @Test
    void everyNamedColourMatchesJavaFx() {
        assertEquals(148, LabelColors.names().size());
        for (String name : LabelColors.names()) {
            assertSame(name);
            assertSame(name.toUpperCase(Locale.ROOT));
        }
    }

    @Test
    void theEdgeCasesMatchJavaFx() {
        for (String color : List.of("#FFF", "#f00a", "#ff000080", "0xff0000", "0XFF0000", "0xff000080", "ff0000",
                "rgb(255,0,0)", "RGB(255, 0, 0)", "rgb(100%,0%,50%)", "rgb(300,-5,10)", "rgb( 1 , 2 , 3 )",
                "rgba(255,0,0,0.5)", "rgba(255,0,0,50%)", "rgba(255,0,0,2)", "rgb(1,2)", "rgb(1.5,2,3)",
                "hsl(120,100%,50%)", "hsl(0,100%,25%)", "hsla(240,100%,50%,0.5)", "hsl(480,100%,50%)",
                "hsl(-30,40%,60%)", "hsl(120,100,50)", "hsl(0,0%,50%)", " red", "red ", "#12", "#1234567",
                "notacolor", "#", "#ggg", "rgb(1,2,3", "rgba(1,2,3)", "hsla(1,2%,3%)")) {
            assertSame(color);
        }
    }

    @Test
    void randomColoursMatchJavaFx() {
        Random random = new Random(42);
        List<String> colors = new ArrayList<>();
        String digits = "0123456789abcdefABCDEF";
        for (int i = 0; i < 2000; i++) {
            int length = new int[] {3, 4, 6, 8}[random.nextInt(4)];
            StringBuilder hex = new StringBuilder(new String[] {"#", "0x", ""}[random.nextInt(3)]);
            for (int d = 0; d < length; d++) hex.append(digits.charAt(random.nextInt(digits.length())));
            colors.add(hex.toString());
        }
        for (int i = 0; i < 1500; i++) {
            String a = component(random);
            String b = component(random);
            String c = component(random);
            colors.add(random.nextBoolean() ? "rgb(" + a + "," + b + "," + c + ")"
                    : "rgba(" + a + ", " + b + ", " + c + ", " + random.nextDouble() * 1.4 + ")");
        }
        for (int i = 0; i < 1500; i++) {
            String hue = String.valueOf(random.nextInt(1000) - 300);
            String s = random.nextInt(130) + "%";
            String l = random.nextDouble() * 120 + "%";
            colors.add(random.nextBoolean() ? "hsl(" + hue + "," + s + "," + l + ")"
                    : "hsla(" + hue + "," + s + "," + l + "," + random.nextDouble() + ")");
        }
        colors.forEach(LabelColorsParityTest::assertSame);
    }

    @Test
    void missingAndInvalidColoursFallBackAsTheDesignerDid() {
        assertEquals(LabelColors.TRANSPARENT, LabelColors.toArgb(null));
        assertEquals(LabelColors.TRANSPARENT, LabelColors.toArgb("  "));
        assertEquals(LabelColors.BLACK, LabelColors.toArgbOrBlack("not-a-color"));
        assertEquals(0xFFFF0000, LabelColors.toArgbOrBlack("Red"));
    }

    private static String component(Random random) {
        return random.nextInt(4) == 0 ? random.nextInt(120) + "%" : String.valueOf(random.nextInt(320) - 30);
    }

    /** Both accept the colour and agree on it, or both reject it. */
    private static void assertSame(String color) {
        Integer expected;
        try {
            Color fx = Color.web(color);
            expected = (int) Math.round(fx.getOpacity() * 255) << 24 | (int) Math.round(fx.getRed() * 255) << 16
                    | (int) Math.round(fx.getGreen() * 255) << 8 | (int) Math.round(fx.getBlue() * 255);
        } catch (IllegalArgumentException rejected) {
            expected = null;
        }
        Integer actual;
        try {
            actual = LabelColors.toArgb(color);
        } catch (IllegalArgumentException rejected) {
            actual = null;
        }
        assertEquals(expected == null ? null : Integer.toHexString(expected),
                actual == null ? null : Integer.toHexString(actual), "colour \"" + color + "\"");
        assertTrue(expected == null || color.isBlank() || actual != null);
    }
}
