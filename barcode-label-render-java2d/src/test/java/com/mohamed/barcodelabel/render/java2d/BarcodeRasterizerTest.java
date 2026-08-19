package com.mohamed.barcodelabel.render.java2d;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarcodeRasterizerTest {
    private final BarcodeRasterizer rasterizer = CompositeBarcodeRasterizer.standard();

    @Test
    void createsReadableCode128() throws Exception {
        String value = "ABC-12345";
        BufferedImage image = rasterizer.rasterize(element(Symbology.CODE_128, value, 70, 25), 827, 295, 300);
        assertEquals(value, decode(image));
    }

    @Test
    void createsReadableQrCode() throws Exception {
        String value = "https://example.com/product/123";
        BufferedImage image = rasterizer.rasterize(element(Symbology.QR_CODE, value, 35, 35), 413, 413, 300);
        assertEquals(value, decode(image));
    }

    private static BarcodeElement element(Symbology type, String value, double width, double height) {
        return new BarcodeElement(
                ElementFrame.create("Test", new RectMm(0, 0, width, height), 0),
                value, type, 0.25, 2.5, true);
    }

    private static String decode(BufferedImage image) throws Exception {
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}

