package com.mohamed.barcodelabel.model.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected values were read from JavaFX's Color.web; the designer module checks the two agree in full. */
class LabelColorsTest {
    @Test
    void namesInAnyCase() {
        assertEquals(0xFFFF0000, LabelColors.toArgb("red"));
        assertEquals(0xFFFF0000, LabelColors.toArgb("RED"));
        assertEquals(0xFF808080, LabelColors.toArgb("gray"));
        assertEquals(0xFF808080, LabelColors.toArgb("grey"));
        assertEquals(0x00000000, LabelColors.toArgb("transparent"));
        assertEquals(148, LabelColors.names().size());
        assertTrue(LabelColors.names().contains("aliceblue"));
    }

    @Test
    void hexInEveryLengthWithAlphaLast() {
        assertEquals(0xFFFFFFFF, LabelColors.toArgb("#FFF"));
        assertEquals(0xAAFF0000, LabelColors.toArgb("#f00a"));
        assertEquals(0xFF123456, LabelColors.toArgb("#123456"));
        assertEquals(0x80FF0000, LabelColors.toArgb("#ff000080"));
        assertEquals(0xFFFF0000, LabelColors.toArgb("0XFF0000"));
        assertEquals(0xFFFF0000, LabelColors.toArgb("ff0000"));
    }

    @Test
    void rgbAndHsbFunctions() {
        assertEquals(0xFFFF0000, LabelColors.toArgb("RGB(255, 0, 0)"));
        assertEquals(0xFFFF0080, LabelColors.toArgb("rgb(100%,0%,50%)"));
        assertEquals(0xFFB2B2B2, LabelColors.toArgb("rgb(70%,70%,70%)"), "rounded as JavaFX's float components");
        assertEquals(0xFFFF000A, LabelColors.toArgb("rgb(300,-5,10)"), "out-of-range values are clamped");
        assertEquals(0x80FF0000, LabelColors.toArgb("rgba(255,0,0,0.5)"));
        assertEquals(0xFFFF0000, LabelColors.toArgb("rgba(255,0,0,2)"));
        assertEquals(0xFF008000, LabelColors.toArgb("hsl(120,100%,50%)"), "hsl is hue, saturation, brightness");
        assertEquals(0xFF400000, LabelColors.toArgb("hsl(0,100%,25%)"));
        assertEquals(0x80000080, LabelColors.toArgb("hsla(240,100%,50%,0.5)"));
        assertEquals(0xFF008000, LabelColors.toArgb("hsl(480,100%,50%)"), "hue wraps around");
        assertEquals(0xFF808080, LabelColors.toArgb("hsl(0,0%,50%)"));
    }

    @Test
    void missingIsTransparentAndInvalidIsRejectedOrBlack() {
        assertEquals(LabelColors.TRANSPARENT, LabelColors.toArgb(null));
        assertEquals(LabelColors.TRANSPARENT, LabelColors.toArgb(" "));
        for (String invalid : new String[] {" red", "#12", "#1234567", "#ggg", "notacolor", "rgb(1,2)",
                "rgb(1.5,2,3)", "rgba(255,0,0,50%)", "hsl(120,100,50)", "rgb(1,2,3"}) {
            assertThrows(IllegalArgumentException.class, () -> LabelColors.toArgb(invalid), invalid);
            assertEquals(LabelColors.BLACK, LabelColors.toArgbOrBlack(invalid), invalid);
        }
    }
}
