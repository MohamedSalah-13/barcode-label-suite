package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.batch.ValidationSeverity;
import com.mohamed.barcodelabel.fx.workbench.ArabicCount;

/** Arabic wording for the batch screen: summaries and validation issues. */
final class BatchTexts {
    private BatchTexts() {}

    /**
     * The counts come first and the file name last: a Latin file name next to a number pulls that number into
     * its left-to-right run, which scrambles the line in a right-to-left label.
     */
    static String fileSummary(String fileName, int rows, int columns, int fields) {
        return ArabicCount.ROWS.count(rows) + "، " + ArabicCount.COLUMNS.count(columns) + " — حقول القالب: " + fields + " — الملف: " + fileName;
    }

    static String ready(int sourceRows, int labels) {
        return "جاهز: سيُطبع " + ArabicCount.LABELS.count(labels) + " من " + ArabicCount.ROWS.afterPreposition(sourceRows);
    }

    /** One line in the issue list, e.g. "خطأ — صف 3: الكمية ليست رقماً صحيحاً: two". */
    static String describe(BatchValidationIssue issue, ColumnMapping mapping) {
        String severity = issue.severity() == ValidationSeverity.ERROR ? "خطأ" : "تنبيه";
        String where = issue.kind().configuration() ? "" : "صف " + issue.sourceRow() + ": ";
        String what = switch (issue.kind()) {
            case UNMAPPED_FIELD -> "الحقل {{" + issue.field() + "}} غير مربوط بأي عمود";
            case MISSING_COLUMN -> "الحقل {{" + issue.field() + "}} مربوط بعمود غير موجود في الملف: " + issue.value();
            case MISSING_QUANTITY_COLUMN -> "عمود الكمية غير موجود في الملف: " + issue.value();
            case QUANTITY_OUT_OF_RANGE -> "الكمية " + issue.value() + " خارج المسموح (من 1 إلى "
                    + mapping.maxCopiesPerRow() + ")";
            case INVALID_QUANTITY -> "الكمية ليست رقماً صحيحاً: " + issue.value();
            case TOTAL_LIMIT_EXCEEDED -> "إجمالي الملصقات سيتجاوز الحد الأقصى (" + issue.value() + ")";
            case OTHER -> issue.message();
        };
        return severity + " — " + where + what;
    }
}
