package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.data.MissingFieldPolicy;
import com.mohamed.barcodelabel.data.TemplateFieldResolver;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.render.java2d.CompositeBarcodeRasterizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Checks each distinct value once per barcode element and stores only its diagnostic, never the image. */
public final class BarcodePreflight {
    /** Resolution used by the desktop print service. */
    private static final int PRINT_DPI = 300;
    private final TemplateFieldResolver resolver = new TemplateFieldResolver();
    private final CompositeBarcodeRasterizer encoder = CompositeBarcodeRasterizer.standard();
    private final List<Check> checks;

    public BarcodePreflight(LabelTemplate template, Map<String, String> columns) {
        checks = template.elements().stream().filter(BarcodeElement.class::isInstance).map(BarcodeElement.class::cast)
                .filter(barcode -> barcode.frame().visible()).map(barcode -> new Check(barcode, columns)).toList();
    }

    public void checkRow(Map<String, ?> record, int row, List<BatchValidationIssue> issues) {
        for (Check check : checks) {
            var barcode = check.barcode();
            boolean blank = false;
            for (var field : check.fields()) {
                if (resolver.resolveText(field.expression(), record, MissingFieldPolicy.EMPTY_STRING).isBlank()) {
                    issues.add(empty(row, field.column(), barcode));
                    blank = true;
                }
            }
            if (blank) continue;
            String value = resolver.resolveText(barcode.dataTemplate(), record, MissingFieldPolicy.FAIL);
            if (value.isBlank()) {
                issues.add(empty(row, check.source(), barcode));
                continue;
            }
            String reason = check.diagnostics().computeIfAbsent(value, data -> encode(barcode, data));
            if (!reason.isEmpty()) {
                issues.add(new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.INVALID_BARCODE,
                        row, check.source(), barcode.symbology().name(), reason));
            }
        }
    }

    public boolean hasBarcodes() { return !checks.isEmpty(); }

    private static BatchValidationIssue empty(int row, String column, BarcodeElement barcode) {
        return new BatchValidationIssue(ValidationSeverity.ERROR, BatchIssueKind.EMPTY_BARCODE,
                row, column, barcode.symbology().name(), "Barcode value is blank");
    }

    private String encode(BarcodeElement barcode, String data) {
        var resolved = new BarcodeElement(barcode.frame(), data, barcode.symbology(), barcode.moduleWidthMm(),
                barcode.quietZoneMm(), barcode.showHumanReadableText());
        try {
            encoder.rasterize(resolved, PRINT_DPI);
            return "";
        } catch (RenderException | RuntimeException error) {
            return error.getMessage() == null || error.getMessage().isBlank()
                    ? error.getClass().getSimpleName() : error.getMessage();
        }
    }

    private record Field(String expression, String column) {}

    private record Check(BarcodeElement barcode, List<Field> fields, String source, Map<String, String> diagnostics) {
        Check(BarcodeElement barcode, Map<String, String> columns) {
            this(barcode, TemplateFields.fieldsIn(barcode.dataTemplate()).stream()
                    .map(name -> new Field("{{" + name + "}}", columns.getOrDefault(name, name))).toList());
        }

        Check(BarcodeElement barcode, List<Field> fields) {
            this(barcode, fields, String.join(", ", fields.stream().map(Field::column).distinct().toList()), new HashMap<>());
        }
    }
}
