package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchIssueKind;
import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.batch.ValidationSeverity;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchTextsTest {
    private static final ColumnMapping MAPPING = new ColumnMapping(Map.of(), null, 1, 20, 500);
    private static final BatchTexts ARABIC = new BatchTexts(UiLanguage.ARABIC);
    private static final BatchTexts ENGLISH = new BatchTexts(UiLanguage.ENGLISH);

    @Test
    void summariesUseTheCountedNouns() {
        assertEquals("جاهز: سيُطبع 10 ملصقات من 4 صفوف", ARABIC.ready(4, 10));
        assertEquals("جاهز: سيُطبع ملصق واحد من صف واحد", ARABIC.ready(1, 1));
        assertEquals("جاهز: سيُطبع ملصقان من صفين", ARABIC.ready(2, 2));
        assertEquals("4 صفوف، 4 أعمدة — حقول القالب: 3 — الملف: products.csv", ARABIC.fileSummary("products.csv", 4, 4, 3));
    }

    @Test
    void theSuggestionMessageCountsTheFields() {
        assertEquals("اقترح البرنامج أعمدة لـ 3 من 3 حقول حسب أسمائها؛ راجعها ثم اضغط «تحقق وجهّز الطباعة»",
                ARABIC.suggested(3, 3));
        assertEquals("اقترح البرنامج أعمدة لـ 1 من حقلين حسب أسمائها؛ راجعها ثم اضغط «تحقق وجهّز الطباعة»",
                ARABIC.suggested(1, 2));
    }

    @Test
    void configurationIssuesHaveNoRowNumber() {
        assertEquals("خطأ — الحقل {{price}} غير مربوط بأي عمود",
                describe(BatchIssueKind.UNMAPPED_FIELD, 1, "price", ""));
        assertEquals("خطأ — الحقل {{sku}} مربوط بعمود غير موجود في الملف: Barcode",
                describe(BatchIssueKind.MISSING_COLUMN, 1, "sku", "Barcode"));
        assertEquals("خطأ — عمود الكمية غير موجود في الملف: Count",
                describe(BatchIssueKind.MISSING_QUANTITY_COLUMN, 1, "quantity", "Count"));
    }

    @Test
    void rowIssuesNameTheRowAndTheLimits() {
        assertEquals("خطأ — صف 2: الكمية 50 خارج المسموح (من 1 إلى 20)",
                describe(BatchIssueKind.QUANTITY_OUT_OF_RANGE, 2, "quantity", "50"));
        assertEquals("خطأ — صف 3: الكمية ليست رقماً صحيحاً: two",
                describe(BatchIssueKind.INVALID_QUANTITY, 3, "quantity", "two"));
        assertEquals("خطأ — صف 9: إجمالي الملصقات سيتجاوز الحد الأقصى (500)",
                describe(BatchIssueKind.TOTAL_LIMIT_EXCEEDED, 9, "quantity", "500"));
    }

    @Test
    void otherIssuesFallBackToTheirMessage() {
        var warning = new BatchValidationIssue(ValidationSeverity.WARNING, 4, "sku", "custom check");
        assertEquals("تنبيه — صف 4: custom check", ARABIC.describe(warning, MAPPING));
    }

    @Test
    void englishSummariesAndIssues() {
        assertEquals("Ready: 10 labels will print from 4 rows", ENGLISH.ready(4, 10));
        assertEquals("Ready: 1 label will print from 1 row", ENGLISH.ready(1, 1));
        assertEquals("4 rows, 1 column — template fields: 3 — file: products.csv", ENGLISH.fileSummary("products.csv", 4, 1, 3));
        assertEquals("Columns were suggested for 1 of 2 fields from their names; check them, then click “Validate and prepare printing”",
                ENGLISH.suggested(1, 2));
        assertEquals("Error — The field {{price}} is not mapped to a column",
                ENGLISH.describe(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.UNMAPPED_FIELD, 1, "price", "", "x"), MAPPING));
        assertEquals("Error — row 2: The quantity 50 is out of range (1 to 20)",
                ENGLISH.describe(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.QUANTITY_OUT_OF_RANGE, 2, "quantity", "50", "x"), MAPPING));
        assertEquals("Warning — row 4: custom check",
                ENGLISH.describe(new BatchValidationIssue(ValidationSeverity.WARNING, 4, "sku", "custom check"), MAPPING));
    }

    @Test
    void bothLanguagesTranslateEveryBatchText() {
        UiTexts arabic = UiTexts.load(BatchTexts.class, "messages", UiLanguage.ARABIC);
        UiTexts english = UiTexts.load(BatchTexts.class, "messages", UiLanguage.ENGLISH);
        assertEquals(arabic.keys(), english.keys());
        english.keys().forEach(key -> assertTrue(english.get(key).codePoints()
                .noneMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC), key));
    }

    private static String describe(BatchIssueKind kind, int row, String field, String value) {
        return ARABIC.describe(new BatchValidationIssue(ValidationSeverity.ERROR, kind, row, field, value, "english"),
                MAPPING);
    }
}
