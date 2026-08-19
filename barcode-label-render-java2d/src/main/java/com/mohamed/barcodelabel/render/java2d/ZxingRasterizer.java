package com.mohamed.barcodelabel.render.java2d;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.render.RenderException;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ZxingRasterizer implements BarcodeRasterizer {
    private static final Set<Symbology> SUPPORTED = Set.of(
            Symbology.GS1_128,
            Symbology.QR_CODE,
            Symbology.DATA_MATRIX,
            Symbology.PDF_417,
            Symbology.AZTEC
    );

    @Override
    public Set<Symbology> supportedSymbologies() {
        return SUPPORTED;
    }

    @Override
    public BufferedImage rasterize(BarcodeElement element, int widthPixels, int heightPixels, double dpi)
            throws RenderException {
        if (widthPixels <= 0 || heightPixels <= 0 || !Double.isFinite(dpi) || dpi <= 0) {
            throw new IllegalArgumentException("barcode dimensions and dpi must be positive");
        }
        if (!supports(element.symbology())) {
            throw new RenderException("ZXing provider does not support: " + element.symbology());
        }

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        int quietModules = Math.max(0, (int) Math.ceil(element.quietZoneMm() / element.moduleWidthMm()));
        hints.put(EncodeHintType.MARGIN, quietModules);
        if (element.symbology() == Symbology.GS1_128) {
            hints.put(EncodeHintType.GS1_FORMAT, true);
        }

        try {
            var matrix = new MultiFormatWriter().encode(
                    element.dataTemplate(), formatOf(element.symbology()), widthPixels, heightPixels, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (IllegalArgumentException | WriterException exception) {
            throw new RenderException("Cannot generate " + element.symbology() + " barcode: "
                    + exception.getMessage(), exception);
        }
    }

    private static BarcodeFormat formatOf(Symbology symbology) {
        return switch (symbology) {
            case GS1_128 -> BarcodeFormat.CODE_128;
            case QR_CODE -> BarcodeFormat.QR_CODE;
            case DATA_MATRIX -> BarcodeFormat.DATA_MATRIX;
            case PDF_417 -> BarcodeFormat.PDF_417;
            case AZTEC -> BarcodeFormat.AZTEC;
            default -> throw new IllegalArgumentException("Unsupported ZXing symbology: " + symbology);
        };
    }
}

