package com.mohamed.barcodelabel.workflow.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvImportServiceTest {
    @TempDir Path directory;

    @Test void readsSemicolonArabicAndQuotedValues() throws Exception {
        Path file = directory.resolve("products.csv");
        Files.writeString(file, "product_name;sku;price\n\"قلم، أزرق\";A-1;12.50\nدفتر;B-2;30");
        CsvDataset data = new CsvImportService().read(file);
        assertEquals(';', data.delimiter());
        assertEquals(2, data.rows().size());
        assertEquals("قلم، أزرق", data.rows().getFirst().get("product_name"));
    }

    @Test void supportsEscapedQuotesAndMultilineFields() {
        var rows = CsvImportService.parse("name,notes\nA,\"line 1\nline \"\"2\"\"\"", ',');
        assertEquals("line 1\nline \"2\"", rows.get(1).get(1));
    }

    @Test void rejectsDuplicateHeaders() throws Exception {
        Path file = directory.resolve("bad.csv");
        Files.writeString(file, "sku,sku\n1,2");
        assertThrows(IllegalArgumentException.class, () -> new CsvImportService().read(file));
    }
}
