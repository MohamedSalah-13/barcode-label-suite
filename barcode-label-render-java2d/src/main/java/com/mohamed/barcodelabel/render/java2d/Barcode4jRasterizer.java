package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.render.RenderException;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.impl.int2of5.ITF14Bean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.impl.upcean.EAN8Bean;
import org.krysalis.barcode4j.impl.upcean.UPCABean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

public final class Barcode4jRasterizer implements BarcodeRasterizer {
    private static final Set<Symbology> SUPPORTED = Set.of(
            Symbology.CODE_128,
            Symbology.CODE_39,
            Symbology.EAN_13,
            Symbology.EAN_8,
            Symbology.UPC_A,
            Symbology.ITF_14
    );

    @Override
    public Set<Symbology> supportedSymbologies() {
        return SUPPORTED;
    }

    @Override
    public BufferedImage rasterize(BarcodeElement element, int widthPixels, int heightPixels, double dpi)
            throws RenderException {
        validateDimensions(widthPixels, heightPixels, dpi);
        if (!supports(element.symbology())) {
            throw new RenderException("Barcode4J does not support: " + element.symbology());
        }

        AbstractBarcodeBean bean = createBean(element.symbology());
        bean.setModuleWidth(element.moduleWidthMm());
        bean.setBarHeight(Math.max(2, pixelsToMm(heightPixels, dpi) - (element.showHumanReadableText() ? 5 : 0)));
        bean.doQuietZone(element.quietZoneMm() > 0);
        bean.setQuietZone(element.quietZoneMm());
        bean.setMsgPosition(element.showHumanReadableText()
                ? HumanReadablePlacement.HRP_BOTTOM
                : HumanReadablePlacement.HRP_NONE);

        try {
            var canvas = new BitmapCanvasProvider(
                    (int) Math.round(dpi), BufferedImage.TYPE_BYTE_BINARY, false, 0);
            bean.generateBarcode(canvas, element.dataTemplate());
            canvas.finish();
            BufferedImage image = canvas.getBufferedImage();
            if (image.getWidth() > widthPixels || image.getHeight() > heightPixels) {
                throw new RenderException("Barcode does not fit its frame. Required "
                        + image.getWidth() + "x" + image.getHeight() + " px, available "
                        + widthPixels + "x" + heightPixels + " px");
            }
            return image;
        } catch (IllegalArgumentException | IOException exception) {
            throw new RenderException("Cannot generate " + element.symbology() + " barcode: "
                    + exception.getMessage(), exception);
        }
    }

    private static AbstractBarcodeBean createBean(Symbology symbology) {
        return switch (symbology) {
            case CODE_128 -> new Code128Bean();
            case CODE_39 -> new Code39Bean();
            case EAN_13 -> new EAN13Bean();
            case EAN_8 -> new EAN8Bean();
            case UPC_A -> new UPCABean();
            case ITF_14 -> new ITF14Bean();
            default -> throw new IllegalArgumentException("Unsupported Barcode4J symbology: " + symbology);
        };
    }

    private static void validateDimensions(int widthPixels, int heightPixels, double dpi) {
        if (widthPixels <= 0 || heightPixels <= 0 || !Double.isFinite(dpi) || dpi <= 0) {
            throw new IllegalArgumentException("barcode dimensions and dpi must be positive");
        }
    }

    private static double pixelsToMm(int pixels, double dpi) {
        return pixels * 25.4 / dpi;
    }
}

