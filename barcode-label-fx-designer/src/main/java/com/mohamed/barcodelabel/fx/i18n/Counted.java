package com.mohamed.barcodelabel.fx.i18n;

/** The things the UI counts, with their Arabic forms and English singular and plural. */
public enum Counted {
    LABELS(ArabicCount.LABELS, "label", "labels"),
    ROWS(ArabicCount.ROWS, "row", "rows"),
    COLUMNS(ArabicCount.COLUMNS, "column", "columns"),
    FIELDS(ArabicCount.FIELDS, "field", "fields"),
    RECORDS(ArabicCount.RECORDS, "record", "records"),
    ELEMENTS(ArabicCount.ELEMENTS, "element", "elements"),
    PAGES(ArabicCount.PAGES, "page", "pages"),
    COPIES(ArabicCount.COPIES, "copy", "copies"),
    PRINTERS(ArabicCount.PRINTERS, "printer", "printers");

    private final ArabicCount.Noun arabic;
    private final String singular;
    private final String plural;

    Counted(ArabicCount.Noun arabic, String singular, String plural) {
        this.arabic = arabic;
        this.singular = singular;
        this.plural = plural;
    }

    String count(UiLanguage language, long n, boolean afterPreposition) {
        return switch (language) {
            case ARABIC -> afterPreposition ? arabic.afterPreposition(n) : arabic.count(n);
            case ENGLISH -> n + " " + (n == 1 ? singular : plural);
        };
    }
}
