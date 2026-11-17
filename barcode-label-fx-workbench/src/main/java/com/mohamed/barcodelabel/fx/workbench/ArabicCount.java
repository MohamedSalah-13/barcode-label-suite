package com.mohamed.barcodelabel.fx.workbench;

/**
 * Counted Arabic nouns for the UI: "ملصق واحد", "ملصقان", "3 ملصقات", "11 ملصقاً", "100 ملصق".
 *
 * <p>1 takes the singular with "واحد"/"واحدة", 2 the dual, 3 to 10 the plural, 11 to 99 the accusative
 * singular, and hundreds follow their last two digits. After a preposition the dual takes its genitive
 * ending, as in "على صفحتين" or "من صفين"; use {@link Noun#afterPreposition(long)} there.</p>
 */
public final class ArabicCount {
    public static final Noun LABELS = masculine("ملصق", "ملصقان", "ملصقات", "ملصقاً");
    public static final Noun ROWS = masculine("صف", "صفان", "صفوف", "صفاً");
    public static final Noun COLUMNS = masculine("عمود", "عمودان", "أعمدة", "عموداً");
    public static final Noun PAGES = feminine("صفحة", "صفحتان", "صفحات", "صفحةً");
    public static final Noun COPIES = feminine("نسخة", "نسختان", "نسخ", "نسخةً");
    public static final Noun PRINTERS = feminine("طابعة", "طابعتان", "طابعات", "طابعةً");

    private ArabicCount() {}

    public static Noun masculine(String singular, String dual, String plural, String accusative) {
        return new Noun(singular, dual, plural, accusative, false);
    }

    public static Noun feminine(String singular, String dual, String plural, String accusative) {
        return new Noun(singular, dual, plural, accusative, true);
    }

    public record Noun(String singular, String dual, String plural, String accusative, boolean feminine) {
        /** The count as a subject, e.g. "سيُطبع ملصقان". */
        public String count(long n) {
            return count(n, dual);
        }

        /** The count after a preposition, where the dual ends in "ين": "على صفحتين", "من صفين". */
        public String afterPreposition(long n) {
            return count(n, dual.endsWith("ان") ? dual.substring(0, dual.length() - 2) + "ين" : dual);
        }

        private String count(long n, String dualForm) {
            if (n == 1) return singular + (feminine ? " واحدة" : " واحد");
            if (n == 2) return dualForm;
            long lastTwo = n % 100;
            if (lastTwo >= 3 && lastTwo <= 10) return n + " " + plural;
            if (lastTwo >= 11) return n + " " + accusative;
            return n + " " + (n == 0 ? plural : singular);
        }
    }
}
