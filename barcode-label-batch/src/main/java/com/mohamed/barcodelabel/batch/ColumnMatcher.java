package com.mohamed.barcodelabel.batch;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Suggests which CSV column fills each template field, and which column holds the quantity, from their names.
 *
 * <p>A field and a column match, strongest first, when their names are the same once case, spaces, "_", "-"
 * and camel case are ignored; when both are known names for the same thing ("product_name" and "Name" or
 * "الصنف", "sku" and "Barcode" or "الكود", "price" and "Retail Price" or "السعر"); or when every word of one is
 * in the other ("price" and "Wholesale Price"). Arabic is compared without diacritics, tatweel or the
 * article "ال", and with أ/إ/آ as ا, ة as ه and ى as ي. Each column fills one field at most; the strongest
 * matches are taken first.</p>
 */
final class ColumnMatcher {
    private static final int SAME_NAME = 3;
    private static final int SAME_MEANING = 2;
    private static final int SHARED_WORDS = 1;

    private static final List<Set<String>> SYNONYMS = List.of(
            keys("name", "product name", "product", "item", "item name", "title", "description",
                    "اسم", "الاسم", "اسم المنتج", "المنتج", "منتج", "الصنف", "صنف", "اسم الصنف"),
            keys("sku", "code", "barcode", "bar code", "item code", "product code", "ean", "upc", "gtin",
                    "كود", "الكود", "باركود", "الباركود", "رمز", "الرمز", "كود الصنف", "رقم الصنف"),
            keys("price", "retail price", "unit price", "sale price", "selling price", "cost", "amount",
                    "سعر", "السعر", "سعر البيع", "سعر الوحدة", "الثمن"));

    private static final Set<String> QUANTITY = keys("qty", "quantity", "count", "copies", "labels",
            "number of labels", "كمية", "الكمية", "عدد", "العدد", "عدد النسخ", "النسخ", "عدد الملصقات");

    private ColumnMatcher() {}

    /** For each field that has a likely column, that column; fields without one are left out. */
    static Map<String, String> fields(List<String> fields, List<String> headers) {
        record Candidate(int field, int header, int score) {}
        List<Candidate> candidates = new ArrayList<>();
        for (int f = 0; f < fields.size(); f++) {
            for (int h = 0; h < headers.size(); h++) {
                int score = score(fields.get(f), headers.get(h));
                if (score > 0) candidates.add(new Candidate(f, h, score));
            }
        }
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparingInt(Candidate::field).thenComparingInt(Candidate::header));

        Map<Integer, Integer> chosen = new HashMap<>();
        Set<Integer> usedHeaders = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (chosen.containsKey(candidate.field()) || usedHeaders.contains(candidate.header())) continue;
            chosen.put(candidate.field(), candidate.header());
            usedHeaders.add(candidate.header());
        }
        var mapping = new LinkedHashMap<String, String>();
        for (int f = 0; f < fields.size(); f++) {
            Integer header = chosen.get(f);
            if (header != null) mapping.put(fields.get(f), headers.get(header));
        }
        return mapping;
    }

    /** The first column whose name says it holds the number of labels per row, or null. */
    static String quantityColumn(List<String> headers) {
        return headers.stream().filter(header -> QUANTITY.contains(key(header))).findFirst().orElse(null);
    }

    static int score(String field, String header) {
        List<String> fieldWords = words(field);
        List<String> headerWords = words(header);
        if (fieldWords.isEmpty() || headerWords.isEmpty()) return 0;
        String fieldKey = String.join("", fieldWords);
        String headerKey = String.join("", headerWords);
        if (fieldKey.equals(headerKey)) return SAME_NAME;
        for (Set<String> meaning : SYNONYMS) {
            if (meaning.contains(fieldKey) && meaning.contains(headerKey)) return SAME_MEANING;
        }
        if (headerWords.containsAll(fieldWords) || fieldWords.containsAll(headerWords)) return SHARED_WORDS;
        return 0;
    }

    private static Set<String> keys(String... names) {
        Set<String> keys = new HashSet<>();
        Arrays.stream(names).map(ColumnMatcher::key).forEach(keys::add);
        return Set.copyOf(keys);
    }

    private static String key(String name) {
        return String.join("", words(name));
    }

    /** The name's words, lower case and normalised, e.g. "retailPrice" or "Retail_Price" -> [retail, price]. */
    static List<String> words(String name) {
        String spaced = name.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
        String plain = Normalizer.normalize(spaced, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\u064B-\\u065F\\u0670\\u0640]", "")
                .replaceAll("[\\u0622\\u0623\\u0625\\u0671]", "ا")
                .replace('ة', 'ه')
                .replace('ى', 'ي');
        List<String> words = new ArrayList<>();
        for (String word : plain.split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty()) continue;
            words.add(word.startsWith("ال") && word.length() > 3 ? word.substring(2) : word);
        }
        return words;
    }
}
