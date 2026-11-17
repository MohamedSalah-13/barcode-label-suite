package com.mohamed.barcodelabel.render;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderRequestTest {
    private static final LabelTemplate TEMPLATE = LabelTemplate.create("Render test",
            PageDefinition.a4Sheet(new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0),
                    new InsetsMm(15.15, 7.25, 15.15, 7.25)),
            List.of());

    @Test
    void missingDataMeansNoFields() {
        assertTrue(new RenderRequest(TEMPLATE, null, 300).data().isEmpty());
    }

    @Test
    void theDataIsCopiedSoLaterChangesDoNotLeakIn() {
        Map<String, Object> data = new HashMap<>(Map.of("sku", "A1"));
        RenderRequest request = new RenderRequest(TEMPLATE, data, 300);
        data.put("sku", "changed");
        assertEquals("A1", request.data().get("sku"));
        assertThrows(UnsupportedOperationException.class, () -> request.data().clear());
    }

    @Test
    void aTemplateAndAFinitePositiveDpiAreRequired() {
        assertThrows(NullPointerException.class, () -> new RenderRequest(null, Map.of(), 300));
        for (double dpi : new double[] {0, -96, Double.NaN, Double.POSITIVE_INFINITY}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> new RenderRequest(TEMPLATE, Map.of(), dpi));
            assertEquals("dpi must be a finite positive number", error.getMessage());
        }
    }

    @Test
    void renderExceptionsKeepTheirCause() {
        IllegalStateException cause = new IllegalStateException("rasterizer failed");
        RenderException error = new RenderException("could not render", cause);
        assertEquals("could not render", error.getMessage());
        assertSame(cause, error.getCause());
        assertEquals("plain", new RenderException("plain").getMessage());
    }

    @Test
    void aLabelRendererCanBeALambda() throws RenderException {
        LabelRenderer<String> renderer = request -> request.template().name() + "@" + request.dpi();
        assertEquals("Render test@300.0", renderer.render(new RenderRequest(TEMPLATE, Map.of(), 300)));
    }
}
