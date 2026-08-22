package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProductionWorkbenchTest {
    @Test
    void nullValuesBecomeEmptyStringsNotTheLiteralWordNull() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sku", "A1");
        record.put("note", null);
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(record));
        assertEquals("", dataset.rows().get(0).get("note"));
    }

    @Test
    void missingKeysAlsoBecomeEmptyStrings() {
        Map<String, Object> first = Map.of("sku", "A1", "price", "10");
        Map<String, Object> second = Map.of("sku", "A2");
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(first, second));
        assertTrue(dataset.headers().containsAll(List.of("sku", "price")));
        assertEquals("", dataset.rows().get(1).get("price"));
    }

    @Test
    void headersAreTheUnionOfAllRecordKeys() {
        Map<String, Object> first = Map.of("a", "1");
        Map<String, Object> second = Map.of("b", "2");
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(first, second));
        assertEquals(2, dataset.headers().size());
        assertTrue(dataset.headers().containsAll(List.of("a", "b")));
    }
}
