package com.mohamed.barcodelabel.batch;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchPreparationResultTest {
    @Test
    void tolerateNullValuesInARecordInsteadOfThrowing() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sku", "A1");
        record.put("note", null);

        var result = new BatchPreparationResult(1, List.of(record), List.of());
        assertEquals(null, result.printRecords().get(0).get("note"));
    }

    @Test
    void printRecordsAreDecoupledFromTheOriginalMutableMap() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sku", "A1");
        var result = new BatchPreparationResult(1, List.of(record), List.of());

        record.put("sku", "changed");
        Map<String, ?> stored = result.printRecords().get(0);
        assertEquals("A1", stored.get("sku"));
        assertThrows(UnsupportedOperationException.class, stored::clear);
    }
}
