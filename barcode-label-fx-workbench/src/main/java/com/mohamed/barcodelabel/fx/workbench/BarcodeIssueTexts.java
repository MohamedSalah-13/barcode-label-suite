package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;

/** Shared diagnostics for mapped batches and CSV files sent straight to the print workspace. */
public final class BarcodeIssueTexts {
    private final UiTexts texts;

    public BarcodeIssueTexts(UiLanguage language) {
        texts = UiTexts.load(UniversalPrintPane.class, "messages", language);
    }

    public String describe(BatchValidationIssue issue) {
        String source = issue.field().isEmpty() ? texts.get("barcode.constant") : texts.get("barcode.column", issue.field());
        return texts.get("barcode." + issue.kind(), source, issue.value(), issue.message());
    }
}
