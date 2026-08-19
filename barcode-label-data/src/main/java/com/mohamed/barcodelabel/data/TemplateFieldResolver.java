package com.mohamed.barcodelabel.data;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.TextElement;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateFieldResolver {
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    public LabelTemplate resolve(LabelTemplate template, Map<String, ?> record, MissingFieldPolicy policy) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(policy, "policy");

        var resolvedElements = new ArrayList<LabelElement>(template.elements().size());
        for (LabelElement element : template.elements()) {
            resolvedElements.add(resolveElement(element, record, policy));
        }
        return template.withElements(resolvedElements);
    }

    public String resolveText(String source, Map<String, ?> record, MissingFieldPolicy policy) {
        Matcher matcher = FIELD_PATTERN.matcher(Objects.requireNonNull(source, "source"));
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = lookup(record, key);
            String replacement = value == null ? missingValue(matcher.group(), key, policy) : String.valueOf(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private LabelElement resolveElement(LabelElement element, Map<String, ?> record, MissingFieldPolicy policy) {
        if (element instanceof TextElement text) {
            return new TextElement(text.frame(), resolveText(text.textTemplate(), record, policy),
                    text.fontFamily(), text.fontSizePoints(), text.color(), text.bold(), text.italic(), text.alignment());
        }
        if (element instanceof BarcodeElement barcode) {
            return new BarcodeElement(barcode.frame(), resolveText(barcode.dataTemplate(), record, policy),
                    barcode.symbology(), barcode.moduleWidthMm(), barcode.quietZoneMm(),
                    barcode.showHumanReadableText());
        }
        return element;
    }

    private static Object lookup(Map<String, ?> record, String path) {
        Object current = record;
        for (String part : path.split("\\.")) {
            current = readPart(current, part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Object readPart(Object source, String part) {
        if (source instanceof Map<?, ?> map) {
            return map.get(part);
        }
        if (source != null && source.getClass().isRecord()) {
            for (RecordComponent component : source.getClass().getRecordComponents()) {
                if (component.getName().equals(part)) {
                    try {
                        return component.getAccessor().invoke(source);
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalArgumentException("Cannot read record field: " + part, exception);
                    }
                }
            }
        }
        return null;
    }

    private static String missingValue(String placeholder, String key, MissingFieldPolicy policy) {
        return switch (policy) {
            case KEEP_PLACEHOLDER -> placeholder;
            case EMPTY_STRING -> "";
            case FAIL -> throw new IllegalArgumentException("Missing template field: " + key);
        };
    }
}

