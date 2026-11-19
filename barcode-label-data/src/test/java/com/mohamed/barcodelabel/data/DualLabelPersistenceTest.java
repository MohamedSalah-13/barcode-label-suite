package com.mohamed.barcodelabel.data;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DualLabelPersistenceTest {
    @Test void oldTemplatesRemainSingleAndDualSettingsRoundTrip() throws Exception {
        var json = new TemplateJsonService();
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var single = LabelTemplate.create("Old", page, List.of());
        String oldJson = json.write(single).replaceAll(",?\\s*\"dualLayout\"\\s*:\\s*null", "");
        assertNull(json.read(oldJson).dualLayout());
        assertEquals(1, json.read(oldJson).schemaVersion());
        var dual = single.withLayout(page, new DualLabelLayout(new InsetsMm(1, 2, 1, 2), 12.0, 1, false, "#123456", false));
        assertEquals(dual, json.read(json.write(dual)));
        assertEquals(dual.dualLayout(), new TemplateFieldResolver().resolve(dual, Map.of(), MissingFieldPolicy.FAIL).dualLayout());
    }
}
