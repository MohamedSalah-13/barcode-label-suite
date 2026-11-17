package com.mohamed.barcodelabel.fx.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The texts of one UI module in one language, read from {@code <name>_<tag>.properties} next to an anchor class,
 * such as {@code messages_en.properties}. The files are UTF-8.
 *
 * <p>Texts take arguments as {@code {0}}, {@code {1}}…, replaced by the arguments as they are: numbers are not
 * grouped or localised, so 100000 stays 100000 and a quote needs no escaping. A missing key throws, so a text
 * that was not translated fails the tests that show it instead of showing its key.</p>
 */
public final class UiTexts {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private final UiLanguage language;
    private final Properties texts;
    private final String source;

    private UiTexts(UiLanguage language, Properties texts, String source) {
        this.language = language;
        this.texts = texts;
        this.source = source;
    }

    /** Loads {@code <name>_<language tag>.properties} from the anchor class's package. */
    public static UiTexts load(Class<?> anchor, String name, UiLanguage language) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(language, "language");
        String file = name + "_" + language.tag() + ".properties";
        String source = anchor.getPackageName().replace('.', '/') + "/" + file;
        try (InputStream stream = anchor.getResourceAsStream(file)) {
            if (stream == null) throw new MissingResourceException("No UI texts at " + source, anchor.getName(), file);
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return new UiTexts(language, properties, source);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read UI texts " + source, exception);
        }
    }

    public UiLanguage language() { return language; }

    /** The text for this key, with {@code {0}}, {@code {1}}… replaced by the arguments. */
    public String get(String key, Object... arguments) {
        String text = texts.getProperty(key);
        if (text == null) throw new MissingResourceException("No text \"" + key + "\" in " + source, source, key);
        if (arguments.length == 0) return text;
        // One pass, so an argument that itself contains "{1}" is shown as it is.
        Matcher placeholder = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (placeholder.find()) {
            int index = Integer.parseInt(placeholder.group(1));
            String value = index < arguments.length ? String.valueOf(arguments[index]) : placeholder.group();
            placeholder.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        return placeholder.appendTail(result).toString();
    }

    /** A count with its noun, as a subject: "10 ملصقات", "ملصقان", "1 label". */
    public String count(Counted noun, long n) { return noun.count(language, n, false); }

    /** A count after a preposition, where the Arabic dual changes: "على صفحتين". English is the same as count. */
    public String countAfterPreposition(Counted noun, long n) { return noun.count(language, n, true); }

    /** The keys this language defines; tests compare them between languages. */
    public Set<String> keys() { return Collections.unmodifiableSet(texts.stringPropertyNames()); }
}
