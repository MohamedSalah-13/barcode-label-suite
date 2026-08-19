package com.mohamed.barcodelabel.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MappingProfileStoreTest {
    @TempDir Path directory;

    @Test void profileRoundTripsUtf8Fields() throws Exception {
        ColumnMapping source = new ColumnMapping(Map.of("اسم_المنتج", "اسم المنتج", "sku", "الكود"), "الكمية", 2, 50, 5000);
        Path file = directory.resolve("products.mapping.properties");
        MappingProfileStore store = new MappingProfileStore();
        store.save(file, source);
        assertEquals(source, store.load(file));
    }
}
