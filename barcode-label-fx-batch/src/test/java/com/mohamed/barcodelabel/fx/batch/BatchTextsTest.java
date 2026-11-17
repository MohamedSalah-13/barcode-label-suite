package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchIssueKind;
import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.batch.ValidationSeverity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchTextsTest {
    private static final ColumnMapping MAPPING = new ColumnMapping(Map.of(), null, 1, 20, 500);

    @Test
    void countsFollowArabicNumberAgreement() {
        assertEquals("ملصق واحد", BatchTexts.LABELS.count(1));
        assertEquals("ملصقان", BatchTexts.LABELS.count(2));
        assertEquals("3 ملصقات", BatchTexts.LABELS.count(3));
        assertEquals("10 ملصقات", BatchTexts.LABELS.count(10));
        assertEquals("11 ملصقاً", BatchTexts.LABELS.count(11));
        assertEquals("99 ملصقاً", BatchTexts.LABELS.count(99));
        assertEquals("100 ملصق", BatchTexts.LABELS.count(100));
        assertEquals("102 ملصق", BatchTexts.LABELS.count(102));
        assertEquals("105 ملصقات", BatchTexts.LABELS.count(105));
        assertEquals("120 ملصقاً", BatchTexts.LABELS.count(120));
        assertEquals("0 ملصقات", BatchTexts.LABELS.count(0));
        assertEquals("صفان", BatchTexts.ROWS.count(2));
        assertEquals("4 أعمدة", BatchTexts.COLUMNS.count(4));
    }

    @Test
    void summariesUseTheCountedNouns() {
        assertEquals("جاهز: سيُطبع 10 ملصقات من 4 صفوف", BatchTexts.ready(4, 10));
        assertEquals("جاهز: سيُطبع ملصق واحد من صف واحد", BatchTexts.ready(1, 1));
        assertEquals("4 صفوف، 4 أعمدة — حقول القالب: 3 — الملف: products.csv", BatchTexts.fileSummary("products.csv", 4, 4, 3));
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
        assertEquals("تنبيه — صف 4: custom check", BatchTexts.describe(warning, MAPPING));
    }

    private static String describe(BatchIssueKind kind, int row, String field, String value) {
        return BatchTexts.describe(new BatchValidationIssue(ValidationSeverity.ERROR, kind, row, field, value, "english"),
                MAPPING);
    }
}
