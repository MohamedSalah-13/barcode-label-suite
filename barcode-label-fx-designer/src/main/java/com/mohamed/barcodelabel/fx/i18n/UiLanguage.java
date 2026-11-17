package com.mohamed.barcodelabel.fx.i18n;

import javafx.geometry.NodeOrientation;

import java.util.Locale;

/**
 * The language of the user interface. Arabic lays the screens out right to left and English left to right; the
 * label surface in the designer is left to right in both, as the label prints.
 */
public enum UiLanguage {
    ARABIC("ar", "العربية", NodeOrientation.RIGHT_TO_LEFT),
    ENGLISH("en", "English", NodeOrientation.LEFT_TO_RIGHT);

    private final String tag;
    private final String nativeName;
    private final NodeOrientation orientation;

    UiLanguage(String tag, String nativeName, NodeOrientation orientation) {
        this.tag = tag;
        this.nativeName = nativeName;
        this.orientation = orientation;
    }

    /** The language tag, such as {@code ar}; it names the language's text files. */
    public String tag() { return tag; }

    /** The language's name in that language, as a language menu lists it: "العربية", "English". */
    public String nativeName() { return nativeName; }

    public NodeOrientation orientation() { return orientation; }

    public Locale locale() { return Locale.forLanguageTag(tag); }

    /** The language with this tag, ignoring case and any region ({@code en-GB} is English), or the fallback. */
    public static UiLanguage fromTag(String tag, UiLanguage fallback) {
        if (tag != null) {
            String language = tag.trim().toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
            for (UiLanguage value : values()) if (value.tag.equals(language)) return value;
        }
        return fallback;
    }

    @Override public String toString() { return nativeName; }
}
