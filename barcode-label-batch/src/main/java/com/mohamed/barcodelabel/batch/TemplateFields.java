package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.TextElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TemplateFields {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    public List<String> scan(LabelTemplate template) {
        Objects.requireNonNull(template, "template");
        var fields = new LinkedHashSet<String>();
        template.elements().forEach(element -> {
            if (element instanceof TextElement text) add(text.textTemplate(), fields);
            if (element instanceof BarcodeElement barcode) add(barcode.dataTemplate(), fields);
        });
        return List.copyOf(new ArrayList<>(fields));
    }

    private static void add(String source, LinkedHashSet<String> target) {
        var matcher = PLACEHOLDER.matcher(source);
        while (matcher.find()) target.add(matcher.group(1));
    }
}
