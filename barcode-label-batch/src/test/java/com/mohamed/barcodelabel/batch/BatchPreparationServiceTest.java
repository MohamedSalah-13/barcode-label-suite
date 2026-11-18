package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.data.MissingFieldPolicy;
import com.mohamed.barcodelabel.data.TemplateFieldResolver;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatchPreparationServiceTest {
    private final BatchPreparationService service = new BatchPreparationService();

    @Test void largeQuantitiesKeepOneMapPerSourceRowAndPreserveOriginalCsvRowNumbers() {
        var data = new CsvDataset(List.of("Name", "Code", "Retail Price", "Qty"),
                List.of(row("Pen", "A1", "12", "100000"), row("Book", "B2", "30", "200000")), ',', List.of(2, 7));
        var mapping = new ColumnMapping(Map.of("product_name", "Name", "sku", "Code", "price", "Retail Price"),
                "Qty", 1, 200000, 300000);
        var result = service.prepare(template(), data, mapping);
        assertTrue(result.ready());
        assertEquals(300000, result.labelCount());
        assertEquals(2, result.sourceRows());
        assertSame(result.printRecords().get(0), result.printRecords().get(99999));
        assertSame(result.printRecords().get(100000), result.printRecords().getLast());
        assertEquals("B2", result.printRecords().get(100000).get("sku"));
        assertEquals(2, result.printSourceRows().get(99999));
        assertEquals(7, result.printSourceRows().get(100000));
        assertEquals(7, result.printSourceRows().getLast());
        var rejected = service.prepare(template(), data, new ColumnMapping(mapping.fields(), "Qty", 1, 200000, 299999));
        assertFalse(rejected.ready());
        assertTrue(rejected.printRecords().isEmpty());
        assertTrue(rejected.printSourceRows().isEmpty());
        assertEquals(7, rejected.issues().getFirst().sourceRow());
    }

    @Test void scansUniqueFieldsInVisualOrder() {
        assertEquals(List.of("product_name", "price", "sku"), new TemplateFields().scan(template()));
    }

    @Test void mapsColumnsAndExpandsPerRowQuantity() {
        CsvDataset data = dataset(List.of(
                row("Blue pen", "A1", "12.5", "2"),
                row("Notebook", "B2", "30", "3")));
        ColumnMapping mapping = new ColumnMapping(Map.of(
                "product_name", "Name", "sku", "Code", "price", "Retail Price"), "Qty", 1, 20, 100);
        BatchPreparationResult result = service.prepare(template(), data, mapping);
        assertTrue(result.ready());
        assertEquals(5, result.labelCount());
        assertEquals("A1", result.printRecords().get(1).get("sku"));
        assertEquals("B2", result.printRecords().get(4).get("sku"));
    }

    @Test void rejectsMissingMappingWithoutPartialPrintRecords() {
        CsvDataset data = dataset(List.of(row("Blue pen", "A1", "12.5", "1")));
        ColumnMapping mapping = new ColumnMapping(Map.of("sku", "Code"), null, 1, 20, 100);
        BatchPreparationResult result = service.prepare(template(), data, mapping);
        assertFalse(result.ready());
        assertTrue(result.printRecords().isEmpty());
        assertEquals(2, result.issues().size());
    }

    @Test void rejectsUnsafeQuantity() {
        CsvDataset data = dataset(List.of(row("Blue pen", "A1", "12.5", "50")));
        ColumnMapping mapping = new ColumnMapping(Map.of(
                "product_name", "Name", "sku", "Code", "price", "Retail Price"), "Qty", 1, 10, 100);
        BatchPreparationResult result = service.prepare(template(), data, mapping);
        assertFalse(result.ready());
        assertTrue(result.printRecords().isEmpty());
    }

    @Test void defaultCopiesRespectTheRowLimitWithAndWithoutAQuantityColumn() {
        var data = dataset(List.of(row("Pen", "A1", "12", " ")));
        var fields = Map.of("product_name", "Name", "sku", "Code", "price", "Retail Price");
        for (String column : new String[] {"Qty", null}) {
            var rejected = service.prepare(template(), data, new ColumnMapping(fields, column, 10, 2, 100));
            assertFalse(rejected.ready());
            assertTrue(rejected.printRecords().isEmpty());
            var issue = rejected.issues().getFirst();
            assertEquals(BatchIssueKind.QUANTITY_OUT_OF_RANGE, issue.kind());
            assertEquals(2, issue.sourceRow());
            assertEquals("10", issue.value());
            var accepted = service.prepare(template(), data, new ColumnMapping(fields, column, 2, 2, 100));
            assertEquals(2, accepted.labelCount());
        }
    }

    @Test void mappedDottedFieldsResolveInThePreparedBarcode() {
        var barcode = new BarcodeElement(ElementFrame.create("barcode", new RectMm(1, 9, 50, 20), 0),
                "{{product.sku}}", Symbology.CODE_128, 0.25, 2, true);
        var template = template().withElements(List.of(barcode));
        var mapping = new ColumnMapping(Map.of("product.sku", "Code"), null, 1, 2, 100);
        var result = service.prepare(template, dataset(List.of(row("Pen", "A1", "12", ""))), mapping);
        assertTrue(result.ready());
        var resolved = new TemplateFieldResolver().resolve(template, result.printRecords().getFirst(), MissingFieldPolicy.FAIL);
        assertEquals("A1", ((BarcodeElement) resolved.elements().getFirst()).dataTemplate());
    }

    @Test void rejectsTheWholeBatchAndReportsEachBadSourceRowAndMappedColumnOnce() {
        var barcode = new BarcodeElement(ElementFrame.create("EAN", new RectMm(1, 1, 60, 24), 0),
                "{{product.sku}}", Symbology.EAN_13, 0.25, 2, false);
        var template = template().withElements(List.of(barcode));
        var mapping = new ColumnMapping(Map.of("product.sku", "Product code"), null, 3, 10, 100);
        var rows = List.of(Map.of("Product code", "4006381333931"), Map.of("Product code", " "),
                Map.of("Product code", "ABC"), Map.of("Product code", "4006381333932"), Map.of("Product code", "ABC"));
        var data = new CsvDataset(List.of("Product code"), rows, ',', List.of(2, 4, 5, 6, 7));
        var result = service.prepare(template, data, mapping);
        assertFalse(result.ready());
        assertTrue(result.printRecords().isEmpty());
        assertEquals(List.of(4, 5, 6, 7), result.issues().stream().map(BatchValidationIssue::sourceRow).toList());
        assertEquals(List.of(BatchIssueKind.EMPTY_BARCODE, BatchIssueKind.INVALID_BARCODE, BatchIssueKind.INVALID_BARCODE,
                BatchIssueKind.INVALID_BARCODE), result.issues().stream().map(BatchValidationIssue::kind).toList());
        result.issues().forEach(issue -> {
            assertEquals("Product code", issue.field());
            assertFalse(issue.message().isBlank());
        });
        var fixed = service.prepare(template, new CsvDataset(data.headers(), List.of(rows.getFirst()), ','), mapping);
        assertTrue(fixed.ready(), fixed.issues().toString());
        assertEquals(3, fixed.labelCount());
    }

    @Test void reportsTheEmptyColumnOfACompositeValueAndSkipsHiddenBarcodes() {
        var frame = ElementFrame.create("QR", new RectMm(1, 1, 22, 22), 0);
        var qr = new BarcodeElement(frame, "item/{{sku}}/{{lot}}", Symbology.QR_CODE, 0.25, 2, false);
        var hidden = new ElementFrame(java.util.UUID.randomUUID(), "Unused", frame.bounds(), 0, 1, false, false);
        var template = template().withElements(List.of(qr, new BarcodeElement(hidden, "", Symbology.EAN_13, 0.25, 2, false)));
        var data = new CsvDataset(List.of("SKU", "Lot"), List.of(Map.of("SKU", "42", "Lot", " ")), ',');
        var mapping = new ColumnMapping(Map.of("sku", "SKU", "lot", "Lot"), null, 1, 10, 100);
        var issues = service.prepare(template, data, mapping).issues();
        assertEquals(1, issues.size());
        assertEquals("Lot", issues.getFirst().field());
        assertEquals(BatchIssueKind.EMPTY_BARCODE, issues.getFirst().kind());
    }

    @Test void automaticMappingIgnoresSpacesUnderscoresAndCase() {
        ColumnMapping mapping = ColumnMapping.automatic(List.of("product_name", "SKU"), List.of("Product Name", "sku"));
        assertEquals("Product Name", mapping.fields().get("product_name"));
        assertEquals("sku", mapping.fields().get("SKU"));
    }

    @Test void configurationIssuesSayWhatIsWrongAndWithWhichColumn() {
        CsvDataset data = dataset(List.of(row("Blue pen", "A1", "12.5", "1")));
        ColumnMapping mapping = new ColumnMapping(Map.of("product_name", "Name", "sku", "Barcode"), "Count", 1, 20, 100);
        List<BatchValidationIssue> issues = service.prepare(template(), data, mapping).issues();

        assertEquals(List.of(BatchIssueKind.UNMAPPED_FIELD, BatchIssueKind.MISSING_COLUMN, BatchIssueKind.MISSING_QUANTITY_COLUMN),
                issues.stream().map(BatchValidationIssue::kind).toList());
        assertEquals("price", issues.get(0).field());
        assertEquals("", issues.get(0).value());
        assertEquals("Barcode", issues.get(1).value());
        assertEquals("Count", issues.get(2).value());
        assertTrue(issues.stream().allMatch(issue -> issue.kind().configuration()));
    }

    @Test void rowIssuesCarryTheRowAndTheRawQuantity() {
        CsvDataset data = dataset(List.of(row("Blue pen", "A1", "12.5", "50"), row("Notebook", "B2", "30", "two")));
        ColumnMapping mapping = new ColumnMapping(Map.of(
                "product_name", "Name", "sku", "Code", "price", "Retail Price"), "Qty", 1, 10, 100);
        List<BatchValidationIssue> issues = service.prepare(template(), data, mapping).issues();

        assertEquals(BatchIssueKind.QUANTITY_OUT_OF_RANGE, issues.get(0).kind());
        assertEquals(2, issues.get(0).sourceRow());
        assertEquals("50", issues.get(0).value());
        assertEquals(BatchIssueKind.INVALID_QUANTITY, issues.get(1).kind());
        assertEquals(3, issues.get(1).sourceRow());
        assertEquals("two", issues.get(1).value());
        assertFalse(issues.get(0).kind().configuration());
    }

    @Test void exceedingTheTotalLimitReportsTheLimit() {
        CsvDataset data = dataset(List.of(row("Blue pen", "A1", "12.5", "6"), row("Notebook", "B2", "30", "6")));
        ColumnMapping mapping = new ColumnMapping(Map.of(
                "product_name", "Name", "sku", "Code", "price", "Retail Price"), "Qty", 1, 10, 10);
        BatchValidationIssue issue = service.prepare(template(), data, mapping).issues().getFirst();

        assertEquals(BatchIssueKind.TOTAL_LIMIT_EXCEEDED, issue.kind());
        assertEquals("10", issue.value());
        assertEquals(3, issue.sourceRow());
    }

    @Test void theMessageOnlyConstructorKeepsWorking() {
        var issue = new BatchValidationIssue(ValidationSeverity.WARNING, 4, "sku", "custom");
        assertEquals(BatchIssueKind.OTHER, issue.kind());
        assertEquals("", issue.value());
        assertEquals("custom", issue.message());
    }

    private static CsvDataset dataset(List<Map<String, String>> rows) {
        return new CsvDataset(List.of("Name", "Code", "Retail Price", "Qty"), rows, ',');
    }

    private static Map<String, String> row(String name, String code, String price, String quantity) {
        var row = new LinkedHashMap<String, String>();
        row.put("Name", name); row.put("Code", code); row.put("Retail Price", price); row.put("Qty", quantity); return row;
    }

    private static LabelTemplate template() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var name = new TextElement(ElementFrame.create("name", new RectMm(1, 1, 50, 7), 0),
                "{{product_name}} — {{price}}", "Arial", 10, "#000000", false, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(ElementFrame.create("barcode", new RectMm(1, 9, 50, 20), 1),
                "{{sku}}", Symbology.CODE_128, 0.25, 2, true);
        return LabelTemplate.create("Products", page, List.of(name, barcode));
    }
}
