package com.mohamed.barcodelabel.batch;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Properties;

public final class MappingProfileStore {
    public void save(Path path, ColumnMapping mapping) throws IOException {
        Properties properties = new Properties();
        mapping.fields().forEach((field, column) -> properties.setProperty("field." + field, column));
        properties.setProperty("quantityColumn", mapping.quantityColumn() == null ? "" : mapping.quantityColumn());
        properties.setProperty("defaultCopies", Integer.toString(mapping.defaultCopies()));
        properties.setProperty("maxCopiesPerRow", Integer.toString(mapping.maxCopiesPerRow()));
        properties.setProperty("maxTotalLabels", Integer.toString(mapping.maxTotalLabels()));
        Path absolute = path.toAbsolutePath();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        try (Writer writer = Files.newBufferedWriter(absolute, StandardCharsets.UTF_8)) {
            properties.store(writer, "Barcode Label CSV mapping profile");
        }
    }

    public ColumnMapping load(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
        var fields = new LinkedHashMap<String, String>();
        properties.stringPropertyNames().stream().filter(name -> name.startsWith("field.")).sorted()
                .forEach(name -> fields.put(name.substring("field.".length()), properties.getProperty(name)));
        String quantity = properties.getProperty("quantityColumn", "").trim();
        return new ColumnMapping(fields, quantity.isEmpty() ? null : quantity,
                integer(properties, "defaultCopies", 1), integer(properties, "maxCopiesPerRow", 999),
                integer(properties, "maxTotalLabels", 100_000));
    }

    private static int integer(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback))); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid mapping profile value: " + key); }
    }
}
