package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.data.RepeatedList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BatchPreparationService {
    private final BarcodeValidationCache barcodeCache;

    public BatchPreparationService() { this(new BarcodeValidationCache()); }

    public BatchPreparationService(BarcodeValidationCache barcodeCache) {
        this.barcodeCache = Objects.requireNonNull(barcodeCache);
    }

    private final TemplateFields templateFields = new TemplateFields();

    public BatchPreparationResult prepare(LabelTemplate template, CsvDataset dataset, ColumnMapping mapping) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(mapping, "mapping");
        List<BatchValidationIssue> issues = validateConfiguration(template, dataset, mapping);
        if (hasErrors(issues)) return new BatchPreparationResult(dataset.rows().size(), List.of(), issues);

        List<Map<String, ?>> records = new ArrayList<>();
        List<Integer> copies = new ArrayList<>();
        List<Integer> sourceRows = new ArrayList<>();
        var barcodes = new BarcodePreflight(template, mapping.fields(), barcodeCache);
        long totalLabels = 0;
        for (int index = 0; index < dataset.rows().size(); index++) {
            Map<String, String> source = dataset.rows().get(index);
            int sourceRow = dataset.sourceRow(index);
            var resolved = new LinkedHashMap<String, String>(source);
            mapping.fields().forEach((field, column) -> resolved.put(field, source.getOrDefault(column, "")));
            barcodes.checkRow(resolved, sourceRow, issues);
            int quantity = quantity(source, mapping, sourceRow, issues);
            if (quantity <= 0) continue;
            if (totalLabels <= mapping.maxTotalLabels() && totalLabels + quantity > mapping.maxTotalLabels()) {
                issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.TOTAL_LIMIT_EXCEEDED,
                        sourceRow, "quantity", String.valueOf(mapping.maxTotalLabels()),
                        "Total labels exceed the safety limit of " + mapping.maxTotalLabels()));
            }
            totalLabels += quantity;
            if (!issues.isEmpty()) continue;
            records.add(resolved);
            copies.add(quantity);
            sourceRows.add(sourceRow);
        }
        if (hasErrors(issues)) return new BatchPreparationResult(dataset.rows().size(), List.of(), issues);
        return new BatchPreparationResult(dataset.rows().size(), RepeatedList.of(records, copies), issues,
                RepeatedList.of(sourceRows, copies));
    }

    private List<BatchValidationIssue> validateConfiguration(LabelTemplate template, CsvDataset dataset, ColumnMapping mapping) {
        List<BatchValidationIssue> issues = new ArrayList<>();
        for (String field : templateFields.scan(template)) {
            String column = mapping.fields().get(field);
            if (column == null) issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.UNMAPPED_FIELD,
                    1, field, "", "No CSV column is mapped to template field {{" + field + "}}"));
            else if (!dataset.headers().contains(column)) issues.add(new BatchValidationIssue(ValidationSeverity.ERROR,
                    BatchIssueKind.MISSING_COLUMN, 1, field, column, "CSV column does not exist: " + column));
        }
        if (mapping.quantityColumn() != null && !dataset.headers().contains(mapping.quantityColumn())) {
            issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.MISSING_QUANTITY_COLUMN, 1,
                    "quantity", mapping.quantityColumn(), "Quantity column does not exist: " + mapping.quantityColumn()));
        }
        return issues;
    }

    private static int quantity(Map<String, String> row, ColumnMapping mapping, int sourceRow, List<BatchValidationIssue> issues) {
        String raw = mapping.quantityColumn() == null ? "" : row.getOrDefault(mapping.quantityColumn(), "").trim();
        if (raw.isEmpty()) raw = Integer.toString(mapping.defaultCopies());
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0 || value > mapping.maxCopiesPerRow()) {
                issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.QUANTITY_OUT_OF_RANGE,
                        sourceRow, "quantity", raw, "Quantity must be between 1 and " + mapping.maxCopiesPerRow() + ": " + raw));
                return -1;
            }
            return value;
        } catch (NumberFormatException exception) {
            issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.INVALID_QUANTITY, sourceRow,
                    "quantity", raw, "Invalid integer quantity: " + raw));
            return -1;
        }
    }

    private static boolean hasErrors(List<BatchValidationIssue> issues) {
        return issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }
}
