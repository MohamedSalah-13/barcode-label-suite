package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import com.mohamed.barcodelabel.render.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class DualLabelRenderTest {
    static LabelTemplate template() {
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 25), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var barcode = new BarcodeElement(ElementFrame.create("Code", new RectMm(0, 2.75, 38, 8.75), 0),
                "{{code}}", Symbology.CODE_128, 0.25, 2.5, true);
        return LabelTemplate.create("Dual", page, List.of(barcode)).withLayout(page, DualLabelLayout.standard());
    }

    @Test void bothCopiesDecodeToTheSameValueAtPrinterDpisAndGuideNeverPrints() throws Exception {
        var renderer = new BufferedImageLabelRenderer();
        for (double dpi : new double[]{203, 300}) {
            var image = renderer.render(new RenderRequest(template(), Map.of("code", "100025"), dpi));
            int mid = image.getHeight() / 2;
            assertEquals("100025", decode(image.getSubimage(0, 0, image.getWidth(), mid)));
            assertEquals("100025", decode(image.getSubimage(0, mid + 1, image.getWidth(), image.getHeight() - mid - 1)));
            for (int x = 0; x < image.getWidth(); x++) assertEquals(0xffffffff, image.getRGB(x, mid));
            var invisible = template().withLayout(template().page(), new DualLabelLayout(DualLabelLayout.standard().margins(),
                    null, 1, false, "#FF0000", false));
            var other = renderer.render(new RenderRequest(invisible, Map.of("code", "100025"), dpi));
            assertArrayEquals(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()),
                    other.getRGB(0, 0, other.getWidth(), other.getHeight(), null, 0, other.getWidth()));
        }
    }

    @Test void overflowIsRejectedInsteadOfClipped() {
        var template = template();
        var smaller = new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 15), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        assertThrows(RenderException.class, () -> new BufferedImageLabelRenderer().render(
                new RenderRequest(template.withLayout(smaller, template.dualLayout()), Map.of("code", "100025"), 203)));
    }

    private static String decode(BufferedImage image) throws Exception {
        int[] rgb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), rgb)))).getText();
    }
}
