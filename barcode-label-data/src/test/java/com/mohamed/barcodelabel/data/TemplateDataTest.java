package com.mohamed.barcodelabel.data;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateDataTest {
    @Test
    void jsonRoundTripPreservesPolymorphicElements() throws Exception {
        LabelTemplate original = sampleTemplate();
        TemplateJsonService json = new TemplateJsonService();

        LabelTemplate restored = json.read(json.write(original));

        assertEquals(original, restored);
        assertInstanceOf(TextElement.class, restored.elements().getFirst());
        assertInstanceOf(BarcodeElement.class, restored.elements().get(1));
    }

    @Test
    void resolvesNestedFieldsInTextAndBarcode() {
        var resolver = new TemplateFieldResolver();
        var resolved = resolver.resolve(sampleTemplate(),
                Map.of("product", Map.of("name", "قهوة عربية"), "sku", "6221234567890"),
                MissingFieldPolicy.FAIL);

        assertEquals("قهوة عربية", ((TextElement) resolved.elements().getFirst()).textTemplate());
        assertEquals("6221234567890", ((BarcodeElement) resolved.elements().get(1)).dataTemplate());
    }

    @Test
    void anExistingNestedPathTakesPrecedenceOverALiteralDottedKey() {
        var data = Map.of("product", Map.of("sku", "nested"), "product.sku", "flat");
        assertEquals("nested", new TemplateFieldResolver().resolveText("{{product.sku}}", data, MissingFieldPolicy.FAIL));
    }

    @Test
    void failsWhenRequiredFieldIsMissing() {
        var resolver = new TemplateFieldResolver();
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(sampleTemplate(), Map.of(), MissingFieldPolicy.FAIL));
    }

    private static LabelTemplate sampleTemplate() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var text = new TextElement(
                ElementFrame.create("Product name", new RectMm(4, 3, 55, 7), 0),
                "{{product.name}}", "Noto Sans Arabic", 11, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(
                ElementFrame.create("SKU", new RectMm(5, 13, 53, 18), 1),
                "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        return LabelTemplate.create("Product label", page, List.of(text, barcode));
    }
}

