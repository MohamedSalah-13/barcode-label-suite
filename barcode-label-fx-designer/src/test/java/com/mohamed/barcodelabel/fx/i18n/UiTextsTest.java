package com.mohamed.barcodelabel.fx.i18n;

import com.mohamed.barcodelabel.fx.designer.LabelDesignerPane;
import javafx.geometry.NodeOrientation;
import org.junit.jupiter.api.Test;

import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTextsTest {
    private final UiTexts arabic = UiTexts.load(LabelDesignerPane.class, "messages", UiLanguage.ARABIC);
    private final UiTexts english = UiTexts.load(LabelDesignerPane.class, "messages", UiLanguage.ENGLISH);

    @Test
    void bothLanguagesTranslateEveryDesignerText() {
        assertEquals(arabic.keys(), english.keys());
        for (String key : english.keys()) {
            assertFalse(arabic.get(key).isBlank(), key);
            assertFalse(english.get(key).isBlank(), key);
            assertFalse(english.get(key).codePoints().anyMatch(UiTextsTest::isArabic), "Arabic in English " + key);
        }
    }

    @Test
    void argumentsReplaceTheirPlaceholdersAsTheyAre() {
        assertEquals("Template saved: label's file.json", english.get("designer.saved", "label's file.json"));
        assertEquals("Selected: 100000", english.get("designer.selected", 100000));
        assertEquals("تم فتح القالب: a.json", arabic.get("designer.opened", "a.json"));
        assertEquals("Undo: {1} $1 \\", english.get("designer.undoAction", "{1} $1 \\", "unused"));
        assertEquals("Selected: {0}", english.get("designer.selected"));
    }

    @Test
    void aMissingTextOrFileFailsInsteadOfShowingItsKey() {
        var missing = assertThrows(MissingResourceException.class, () -> english.get("no.such.key"));
        assertTrue(missing.getMessage().contains("no.such.key"), missing.getMessage());
        assertThrows(MissingResourceException.class, () -> UiTexts.load(UiTextsTest.class, "nothing", UiLanguage.ENGLISH));
    }

    @Test
    void countsUseEachLanguagesNumberRules() {
        assertEquals("1 label", english.count(Counted.LABELS, 1));
        assertEquals("2 labels", english.countAfterPreposition(Counted.LABELS, 2));
        assertEquals("0 copies", english.count(Counted.COPIES, 0));
        assertEquals("ملصقان", arabic.count(Counted.LABELS, 2));
        assertEquals("صفحتين", arabic.countAfterPreposition(Counted.PAGES, 2));
        assertEquals("عنصر واحد", arabic.count(Counted.ELEMENTS, 1));
        assertEquals("3 عناصر", arabic.count(Counted.ELEMENTS, 3));
    }

    @Test
    void languagesHaveATagAnOrientationAndTheirOwnName() {
        assertEquals(UiLanguage.ENGLISH, UiLanguage.fromTag("en-GB", UiLanguage.ARABIC));
        assertEquals(UiLanguage.ARABIC, UiLanguage.fromTag(" AR ", UiLanguage.ENGLISH));
        assertEquals(UiLanguage.ARABIC, UiLanguage.fromTag("fr", UiLanguage.ARABIC));
        assertEquals(UiLanguage.ENGLISH, UiLanguage.fromTag(null, UiLanguage.ENGLISH));
        assertEquals(NodeOrientation.RIGHT_TO_LEFT, UiLanguage.ARABIC.orientation());
        assertEquals(NodeOrientation.LEFT_TO_RIGHT, UiLanguage.ENGLISH.orientation());
        assertEquals("العربية", UiLanguage.ARABIC.toString());
        assertEquals("en", UiLanguage.ENGLISH.locale().getLanguage());
    }

    static boolean isArabic(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.ARABIC;
    }
}
