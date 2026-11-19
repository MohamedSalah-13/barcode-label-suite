package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.print.UniversalPagePlanner;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DualLabelBatchTest {
    @Test void quantitiesCountWholeLabelsAndInvalidLayoutStopsPreparation() {
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var code = new BarcodeElement(ElementFrame.create("Code", new RectMm(0, 2.75, 38, 8.75), 0),
                "{{code}}", Symbology.CODE_128, 0.25, 2.5, true);
        var template = LabelTemplate.create("Dual", page, List.of(code)).withLayout(page, DualLabelLayout.standard());
        var data = new CsvDataset(List.of("Code", "Qty"), List.of(
                Map.of("Code", "100025", "Qty", "2"), Map.of("Code", "100026", "Qty", "3")), ',');
        var mapping = new ColumnMapping(Map.of("code", "Code"), "Qty", 1, 10, 100);
        var service = new BatchPreparationService();
        var result = service.prepare(template, data, mapping);
        assertTrue(result.ready());
        assertEquals(5, result.labelCount());
        var pages = new UniversalPagePlanner().plan(page, result.labelCount(), 0);
        assertEquals(5, pages.size());
        assertEquals("100025", result.printRecords().get(1).get("code"));
        assertEquals("100026", result.printRecords().get(2).get("code"));
        assertEquals(1, pages.getFirst().placements().size());
        var tooSmall = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 15), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var rejected = service.prepare(template.withLayout(tooSmall, template.dualLayout()), data, mapping);
        assertFalse(rejected.ready());
        assertTrue(rejected.printRecords().isEmpty());
        assertEquals(BatchIssueKind.INVALID_LAYOUT, rejected.issues().getFirst().kind());
    }
}
