package com.mohamed.barcodelabel.batch;

/** What a {@link BatchValidationIssue} is about, so a UI can describe it in its own language. */
public enum BatchIssueKind {
    /** A template field has no CSV column; {@code value} is empty. */
    UNMAPPED_FIELD(true),
    /** A field is mapped to a column the CSV does not have; {@code value} is the column. */
    MISSING_COLUMN(true),
    /** The quantity column is not in the CSV; {@code value} is the column. */
    MISSING_QUANTITY_COLUMN(true),
    /** A row's quantity is outside 1..maxCopiesPerRow; {@code value} is the raw quantity. */
    QUANTITY_OUT_OF_RANGE(false),
    /** A row's quantity is not an integer; {@code value} is the raw quantity. */
    INVALID_QUANTITY(false),
    /** Adding a row would exceed maxTotalLabels; {@code value} is the limit. */
    TOTAL_LIMIT_EXCEEDED(false),
    /** The mapped CSV column is blank, or the barcode has an empty constant. */
    EMPTY_BARCODE(false),
    /** The printer's encoder rejected the resolved barcode. */
    INVALID_BARCODE(false),
    /** Anything else; only {@code message} describes it. */
    OTHER(false),
    /** The shared design cannot fit both halves of the label. */
    INVALID_LAYOUT(true);

    private final boolean configuration;

    BatchIssueKind(boolean configuration) {
        this.configuration = configuration;
    }

    /** True when the issue is about the mapping as a whole rather than one CSV row. */
    public boolean configuration() {
        return configuration;
    }
}
