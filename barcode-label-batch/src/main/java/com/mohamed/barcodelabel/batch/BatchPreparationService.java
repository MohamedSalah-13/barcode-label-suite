package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BatchPreparationService {
    private final TemplateFields templateFields = new TemplateFields();

    public BatchPreparationResult prepare(LabelTemplate template, CsvDataset dataset, ColumnMapping mapping) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(mapping, "mapping");
        List<BatchValidationIssue> issues = validateConfiguration(template, dataset, mapping);
        if (hasErrors(issues)) return new BatchPreparationResult(dataset.rows().size(), List.of(), issues);

        List<Map<String, ?>> records = new ArrayList<>();
        for (int index = 0; index < dataset.rows().size(); index++) {
            Map<String, String> source = dataset.rows().get(index);
            int sourceRow = index + 2;
            int quantity = quantity(source, mapping, sourceRow, issues);
            if (quantity <= 0) continue;
            if ((long) records.size() + quantity > mapping.maxTotalLabels()) {
                issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, sourceRow, "quantity",
                        "Total labels exceed the safety limit of " + mapping.maxTotalLabels()));
                break;
            }
            var resolved = new LinkedHashMap<String, Object>();
            resolved.putAll(source);
            mapping.fields().forEach((field, column) -> resolved.put(field, source.getOrDefault(column, "")));
            for (int copy = 0; copy < quantity; copy++) records.add(Map.copyOf(resolved));
        }
        if (hasErrors(issues)) records = List.of();
        return new BatchPreparationResult(dataset.rows().size(), records, issues);
    }

    private List<BatchValidationIssue> validateConfiguration(LabelTemplate template, CsvDataset dataset, ColumnMapping mapping) {
        List<BatchValidationIssue> issues = new ArrayList<>();
        for (String field : templateFields.scan(template)) {
            String column = mapping.fields().get(field);
            if (column == null) issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, 1, field, "No CSV column is mapped to template field {{" + field + "}}"));
            else if (!dataset.headers().contains(column)) issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, 1, field, "CSV column does not exist: " + column));
        }
        if (mapping.quantityColumn() != null && !dataset.headers().contains(mapping.quantityColumn())) {
            issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, 1, "quantity", "Quantity column does not exist: " + mapping.quantityColumn()));
        }
        return issues;
    }

    private static int quantity(Map<String, String> row, ColumnMapping mapping, int sourceRow, List<BatchValidationIssue> issues) {
        String raw = mapping.quantityColumn() == null ? "" : row.getOrDefault(mapping.quantityColumn(), "").trim();
        if (raw.isEmpty()) return mapping.defaultCopies();
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0 || value > mapping.maxCopiesPerRow()) {
                issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, sourceRow, "quantity",
                        "Quantity must be between 1 and " + mapping.maxCopiesPerRow() + ": " + raw));
                return -1;
            }
            return value;
        } catch (NumberFormatException exception) {
            issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, sourceRow, "quantity", "Invalid integer quantity: " + raw));
            return -1;
        }
    }

    private static boolean hasErrors(List<BatchValidationIssue> issues) {
        return issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }
}
