package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.batch.ValidationSeverity;

/** Arabic wording for the batch screen: counted nouns and validation issues. */
final class BatchTexts {
    static final Noun ROWS = new Noun("صف", "صفان", "صفوف", "صفاً");
    static final Noun COLUMNS = new Noun("عمود", "عمودان", "أعمدة", "عموداً");
    static final Noun LABELS = new Noun("ملصق", "ملصقان", "ملصقات", "ملصقاً");

    private BatchTexts() {}

    /**
     * A masculine noun in the forms Arabic counting needs: 1 and 2 use the singular with "واحد" and the dual,
     * 3 to 10 the plural, 11 to 99 the accusative singular, and hundreds follow their last two digits.
     */
    record Noun(String singular, String dual, String plural, String accusative) {
        String count(long n) {
            if (n == 1) return singular + " واحد";
            if (n == 2) return dual;
            long lastTwo = n % 100;
            if (lastTwo >= 3 && lastTwo <= 10) return n + " " + plural;
            if (lastTwo >= 11) return n + " " + accusative;
            return n + " " + (n == 0 ? plural : singular);
        }
    }

    /**
     * The counts come first and the file name last: a Latin file name next to a number pulls that number into
     * its left-to-right run, which scrambles the line in a right-to-left label.
     */
    static String fileSummary(String fileName, int rows, int columns, int fields) {
        return ROWS.count(rows) + "، " + COLUMNS.count(columns) + " — حقول القالب: " + fields + " — الملف: " + fileName;
    }

    static String ready(int sourceRows, int labels) {
        return "جاهز: سيُطبع " + LABELS.count(labels) + " من " + ROWS.count(sourceRows);
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
