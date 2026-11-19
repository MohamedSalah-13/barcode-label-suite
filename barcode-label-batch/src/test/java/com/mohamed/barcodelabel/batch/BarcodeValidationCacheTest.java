package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class BarcodeValidationCacheTest {
    private static BarcodeElement barcode() {
        return new BarcodeElement(ElementFrame.create("barcode", new RectMm(1, 1, 50, 20), 1),
                "{{sku}}", Symbology.CODE_128, .25, 2, true);
    }
    private static LabelTemplate template(BarcodeElement barcode) {
        return LabelTemplate.create("Products", new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page(), List.of(barcode));
    }

    @Test void unchangedValuesReuseSuccessAndFailuresAndChangedValuesReencode() {
        var cache = new BarcodeValidationCache();
        var barcode = barcode();
        var template = template(barcode);
        var calls = new AtomicInteger();
        var scope = cache.scope(template);
        assertEquals("", scope.diagnostic(barcode, "A", () -> { calls.incrementAndGet(); return ""; }));
        assertSame(scope, cache.scope(template));
        assertEquals("", scope.diagnostic(barcode, "A", () -> { throw new AssertionError(); }));
        assertEquals("invalid", scope.diagnostic(barcode, "B", () -> { calls.incrementAndGet(); return "invalid"; }));
        assertEquals("invalid", scope.diagnostic(barcode, "B", () -> { throw new AssertionError(); }));
        assertEquals(2, calls.get());
    }

    @Test void templateChangesInvalidateAndLateOldChecksCannotPolluteNewScope() {
        var cache = new BarcodeValidationCache();
        var barcode = barcode();
        var template = template(barcode);
        var old = cache.scope(template);
        old.diagnostic(barcode, "A", () -> "old");
        var changed = LabelTemplate.create("Changed", template.page(), template.elements());
        var current = cache.scope(changed);
        assertNotSame(old, current);
        old.diagnostic(barcode, "B", () -> "late old result");
        assertEquals("new", current.diagnostic(barcode, "B", () -> "new"));
        assertNotSame(old, cache.scope(template));
    }

    @Test void boundedCacheEvictsLeastRecentlyUsedEntry() {
        assertThrows(IllegalArgumentException.class, () -> new BarcodeValidationCache(0));
        var barcode = barcode();
        var scope = new BarcodeValidationCache(2).scope(template(barcode));
        scope.diagnostic(barcode, "A", () -> "a");
        scope.diagnostic(barcode, "B", () -> "b");
        scope.diagnostic(barcode, "A", () -> { throw new AssertionError(); });
        scope.diagnostic(barcode, "C", () -> "c");
        assertEquals("rechecked", scope.diagnostic(barcode, "B", () -> "rechecked"));
    }

    @Test void concurrentChecksComputeEachValueOnlyOnce() throws Exception {
        var barcode = barcode();
        var scope = new BarcodeValidationCache().scope(template(barcode));
        var calls = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var jobs = new ArrayList<Future<String>>();
            for (int i = 0; i < 20; i++) jobs.add(executor.submit(() -> scope.diagnostic(barcode, "A", () -> {
                calls.incrementAndGet(); return "";
            })));
            for (var job : jobs) assertEquals("", job.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, calls.get());
    }

    @Test void sharedChecksResolveCurrentDataAndRegenerateRowAndColumnDiagnostics() {
        var cache = new BarcodeValidationCache();
        var barcode = barcode();
        var template = template(barcode);
        cache.scope(template).diagnostic(barcode, "bad", () -> "encoding failed");
        var service = new BatchPreparationService(cache);
        var dataset = new CsvDataset(List.of("Code"), List.of(Map.of("Code", "bad")), ',', List.of(42));
        var prepared = service.prepare(template, dataset, new ColumnMapping(Map.of("sku", "Code"), null, 1, 10, 100));
        assertFalse(prepared.ready());
        assertEquals("encoding failed", prepared.issues().getFirst().message());
        assertEquals(42, prepared.issues().getFirst().sourceRow());
        var issues = new ArrayList<BatchValidationIssue>();
        var record = new HashMap<String, String>();
        record.put("sku", "bad");
        var check = new BarcodePreflight(template, Map.of("sku", "Other"), cache);
        check.checkRow(record, 7, issues);
        assertEquals("encoding failed", issues.getFirst().message());
        assertEquals("Other", issues.getFirst().field());
        assertEquals(7, issues.getFirst().sourceRow());
        issues.clear();
        record.put("sku", "good");
        check.checkRow(record, 8, issues);
        assertTrue(issues.isEmpty());
        record.put("sku", " ");
        check.checkRow(record, 9, issues);
        assertEquals(BatchIssueKind.EMPTY_BARCODE, issues.getFirst().kind());
    }
}
