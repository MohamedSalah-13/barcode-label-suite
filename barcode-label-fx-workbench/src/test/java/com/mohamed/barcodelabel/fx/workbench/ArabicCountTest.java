package com.mohamed.barcodelabel.fx.workbench;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArabicCountTest {
    @Test
    void countsFollowArabicNumberAgreement() {
        assertEquals("ملصق واحد", ArabicCount.LABELS.count(1));
        assertEquals("ملصقان", ArabicCount.LABELS.count(2));
        assertEquals("3 ملصقات", ArabicCount.LABELS.count(3));
        assertEquals("10 ملصقات", ArabicCount.LABELS.count(10));
        assertEquals("11 ملصقاً", ArabicCount.LABELS.count(11));
        assertEquals("99 ملصقاً", ArabicCount.LABELS.count(99));
        assertEquals("100 ملصق", ArabicCount.LABELS.count(100));
        assertEquals("102 ملصق", ArabicCount.LABELS.count(102));
        assertEquals("105 ملصقات", ArabicCount.LABELS.count(105));
        assertEquals("120 ملصقاً", ArabicCount.LABELS.count(120));
        assertEquals("0 ملصقات", ArabicCount.LABELS.count(0));
        assertEquals("4 أعمدة", ArabicCount.COLUMNS.count(4));
    }

    @Test
    void feminineNounsUseTheFeminineOne() {
        assertEquals("صفحة واحدة", ArabicCount.PAGES.count(1));
        assertEquals("طابعة واحدة", ArabicCount.PRINTERS.count(1));
        assertEquals("نسختان", ArabicCount.COPIES.count(2));
        assertEquals("5 طابعات", ArabicCount.PRINTERS.count(5));
        assertEquals("12 صفحةً", ArabicCount.PAGES.count(12));
    }

    @Test
    void theDualTakesItsGenitiveEndingAfterAPreposition() {
        assertEquals("صفحتين", ArabicCount.PAGES.afterPreposition(2));
        assertEquals("صفين", ArabicCount.ROWS.afterPreposition(2));
        assertEquals("طابعتين", ArabicCount.PRINTERS.afterPreposition(2));
        assertEquals("صفحة واحدة", ArabicCount.PAGES.afterPreposition(1));
        assertEquals("7 صفحات", ArabicCount.PAGES.afterPreposition(7));
    }
}
