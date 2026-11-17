package com.mohamed.barcodelabel.batch;

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
