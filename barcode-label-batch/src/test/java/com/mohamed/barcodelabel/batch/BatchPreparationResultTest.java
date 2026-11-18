package com.mohamed.barcodelabel.batch;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.mohamed.barcodelabel.workflow.data.RepeatedList;

class BatchPreparationResultTest {
    @Test void snapshotsEachLogicalRowOnceAndKeepsExpandedListCompatibility() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("sku", "A1"); first.put("note", null);
        List<Map<String, ?>> values = List.of(first, Map.of("sku", "B2"));
        var records = RepeatedList.of(values, List.of(2, 3));
        var sourceRows = RepeatedList.of(List.of(2, 5), List.of(2, 3));
        var result = new BatchPreparationResult(2, records, List.of(), sourceRows);
        first.put("sku", "changed");
        assertEquals(5, result.labelCount());
        assertEquals("A1", result.printRecords().getFirst().get("sku"));
        assertEquals(null, result.printRecords().getFirst().get("note"));
        assertSame(result.printRecords().get(0), result.printRecords().get(1));
        assertSame(result.printRecords().get(2), result.printRecords().get(4));
        assertEquals(2, assertInstanceOf(RepeatedList.class, result.printRecords()).values().size());
        assertEquals(List.of(2, 2, 5, 5, 5), result.printSourceRows());
        assertThrows(UnsupportedOperationException.class, () -> result.printRecords().getFirst().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.printRecords().clear());
        assertThrows(IllegalArgumentException.class, () -> new BatchPreparationResult(2, records, List.of(), List.of(2)));
    }
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
