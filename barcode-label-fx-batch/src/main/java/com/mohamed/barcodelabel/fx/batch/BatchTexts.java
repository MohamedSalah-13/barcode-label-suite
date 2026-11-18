package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.batch.ValidationSeverity;
import com.mohamed.barcodelabel.fx.i18n.Counted;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.fx.workbench.BarcodeIssueTexts;

/** The batch screens' wording in one language: summaries and validation issues. */
final class BatchTexts {
    private final UiTexts texts;
    private final BarcodeIssueTexts barcodeTexts;

    BatchTexts(UiLanguage language) {
        texts = UiTexts.load(BatchTexts.class, "messages", language);
        barcodeTexts = new BarcodeIssueTexts(language);
    }

    UiLanguage language() { return texts.language(); }

    String get(String key, Object... arguments) { return texts.get(key, arguments); }

    String fileSummary(String fileName, int rows, int columns, int fields) {
        return get("batch.fileSummary", texts.count(Counted.ROWS, rows), texts.count(Counted.COLUMNS, columns), fields, fileName);
    }

    /** Shown when the column names suggested a column for some fields, which the user should check. */
    String suggested(int matched, int fields) {
        return get("batch.suggested", matched, texts.countAfterPreposition(Counted.FIELDS, fields));
    }

    String ready(int sourceRows, int labels) {
        return get("batch.ready", texts.count(Counted.LABELS, labels), texts.countAfterPreposition(Counted.ROWS, sourceRows));
    }

    /** One line in the issue list, e.g. "خطأ — صف 3: الكمية ليست رقماً صحيحاً: two". */
    String describe(BatchValidationIssue issue, ColumnMapping mapping) {
        String severity = get(issue.severity() == ValidationSeverity.ERROR ? "issue.error" : "issue.warning");
        String what = switch (issue.kind()) {
            case UNMAPPED_FIELD -> get("issue.UNMAPPED_FIELD", issue.field());
            case MISSING_COLUMN -> get("issue.MISSING_COLUMN", issue.field(), issue.value());
            case MISSING_QUANTITY_COLUMN -> get("issue.MISSING_QUANTITY_COLUMN", issue.value());
            case QUANTITY_OUT_OF_RANGE -> get("issue.QUANTITY_OUT_OF_RANGE", issue.value(), mapping.maxCopiesPerRow());
            case INVALID_QUANTITY -> get("issue.INVALID_QUANTITY", issue.value());
            case EMPTY_BARCODE, INVALID_BARCODE -> barcodeTexts.describe(issue);
            case TOTAL_LIMIT_EXCEEDED -> get("issue.TOTAL_LIMIT_EXCEEDED", issue.value());
            case OTHER -> issue.message();
        };
        return issue.kind().configuration() ? get("issue.configuration", severity, what)
                : get("issue.inRow", severity, issue.sourceRow(), what);
    }
}
