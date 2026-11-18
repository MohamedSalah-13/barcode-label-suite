package com.mohamed.barcodelabel.workflow.csv;

import com.mohamed.barcodelabel.workflow.data.RepeatedList;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CsvDatasetTest {
    @Test void repeatedRowsStayCompactAndSnapshotMutableInputs() {
        var value = new LinkedHashMap<>(Map.of("sku", "A1"));
        List<Map<String, String>> values = List.of(value, Map.of("sku", "B2"));
        var dataset = new CsvDataset(List.of("sku"), RepeatedList.of(values, List.of(500000, 500000)), ',',
                RepeatedList.of(List.of(2, 9), List.of(500000, 500000)));
        value.put("sku", "changed");
        assertEquals(1000000, dataset.rows().size());
        assertEquals("A1", dataset.rows().getFirst().get("sku"));
        assertEquals(2, assertInstanceOf(RepeatedList.class, dataset.rows()).values().size());
        var print = dataset.printRecords();
        assertEquals(2, assertInstanceOf(RepeatedList.class, print).values().size());
        assertSame(print.get(0), print.get(499999));
        assertSame(print.get(500000), print.getLast());
        assertEquals(2, dataset.sourceRow(499999));
        assertEquals(9, dataset.sourceRow(500000));
        assertThrows(UnsupportedOperationException.class, () -> print.getFirst().clear());
        assertThrows(UnsupportedOperationException.class, print::clear);
    }

    @Test void ordinaryDatasetsKeepTheirExistingSnapshotAndSourceRowBehavior() {
        var values = new ArrayList<Map<String, String>>(List.of(Map.of("sku", "A1")));
        var rows = new ArrayList<>(List.of(5));
        var dataset = new CsvDataset(List.of("sku"), values, ',', rows);
        values.clear(); rows.clear();
        assertEquals(1, dataset.rows().size());
        assertEquals(List.of(Map.of("sku", "A1")), dataset.printRecords());
        assertEquals(5, dataset.sourceRow(0));
        assertEquals(2, new CsvDataset(dataset.headers(), dataset.rows(), ',').sourceRow(0));
        assertThrows(IllegalArgumentException.class,
                () -> new CsvDataset(dataset.headers(), dataset.rows(), ',', List.of(2, 3)));
    }
}
